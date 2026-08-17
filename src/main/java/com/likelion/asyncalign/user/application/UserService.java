package com.likelion.asyncalign.user.application;

import java.util.List;
import java.util.UUID;
import java.time.ZoneId;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.user.dto.UserResponse;
import com.likelion.asyncalign.user.dto.UserSummaryResponse;
import com.likelion.asyncalign.user.dto.UpdateProfileRequest;
import com.likelion.asyncalign.user.dto.UpdateWorkContextRequest;
import com.likelion.asyncalign.user.domain.WorkRole;
import com.likelion.asyncalign.storage.FileStorageService;
import com.likelion.asyncalign.workspace.domain.WorkspaceMemberRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    public UserService(
            UserRepository userRepository,
            FileStorageService fileStorageService,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public UserResponse getMe(UUID userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        String customRole = request.customRole() == null ? null : request.customRole().trim();
        if (request.role() == WorkRole.OTHER && (customRole == null || customRole.isBlank())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "기타 역할을 선택하면 역할을 직접 입력해야 합니다.");
        }

        User user = getUser(userId);
        user.updateProfile(
                request.displayName().trim(),
                request.role(),
                customRole,
                request.preferredLanguage().toLowerCase());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateWorkContext(UUID userId, UpdateWorkContextRequest request) {
        try {
            ZoneId.of(request.timeZoneId());
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효한 IANA 타임존을 입력해 주세요.");
        }
        if (!request.workStart().isBefore(request.workEnd())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "근무 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        User user = getUser(userId);
        user.updateWorkContext(
                request.timeZoneId(),
                request.workStart(),
                request.workEnd(),
                request.workDays());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse uploadProfileImage(UUID userId, MultipartFile file) {
        User user = getUser(userId);
        user.updateProfileImageUrl(fileStorageService.storeProfileImage(userId, file));
        return UserResponse.from(user);
    }

    public List<UserSummaryResponse> search(
            UUID currentUserId,
            UUID workspaceId,
            String query,
            int size
    ) {
        int safeSize = Math.clamp(size, 1, 50);
        String keyword = query == null ? "" : query.trim();
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKSPACE_NOT_FOUND, "워크스페이스를 찾을 수 없습니다."));
        workspaceMemberRepository.findMembership(workspaceId, currentUserId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.WORKSPACE_ACCESS_DENIED,
                        "워크스페이스 멤버만 사용자를 검색할 수 있습니다."));
        return workspaceMemberRepository
                .searchMembers(workspaceId, currentUserId, keyword, PageRequest.of(0, safeSize))
                .stream()
                .map(member -> UserSummaryResponse.from(member.getUser()))
                .toList();
    }
}

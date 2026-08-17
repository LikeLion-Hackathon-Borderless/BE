package com.likelion.asyncalign.workspace.application;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.invitation.domain.InvitationStatus;
import com.likelion.asyncalign.invitation.domain.WorkspaceInvitationRepository;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.user.dto.UpdateWorkContextRequest;
import com.likelion.asyncalign.workspace.domain.Workspace;
import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import com.likelion.asyncalign.workspace.domain.WorkspaceMemberRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRole;
import com.likelion.asyncalign.workspace.dto.CreateWorkspaceRequest;
import com.likelion.asyncalign.workspace.dto.WorkspaceMemberResponse;
import com.likelion.asyncalign.workspace.dto.WorkspaceResponse;
import com.likelion.asyncalign.workspace.dto.WorkspaceWorkContextResponse;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final int MAX_MEMBERS = 100;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceInvitationRepository invitationRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceInvitationRepository invitationRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    public WorkspaceResponse create(UUID userId, CreateWorkspaceRequest request) {
        User creator = getUser(userId);
        String domain = request.organizationDomain() == null
                ? null
                : request.organizationDomain().trim().toLowerCase(Locale.ROOT);
        Workspace workspace = workspaceRepository.save(
                new Workspace(request.name().trim(), domain, creator));
        WorkspaceMember membership = memberRepository.save(
                new WorkspaceMember(workspace, creator, WorkspaceRole.OWNER));
        creator.completeOnboarding();
        return WorkspaceResponse.of(workspace, membership.getRole(), 1);
    }

    public List<WorkspaceResponse> getMine(UUID userId) {
        return memberRepository.findActiveMemberships(userId).stream()
                .map(member -> WorkspaceResponse.of(
                        member.getWorkspace(),
                        member.getRole(),
                        memberRepository.countByWorkspaceId(member.getWorkspace().getId())))
                .toList();
    }

    public WorkspaceResponse get(UUID userId, UUID workspaceId) {
        Workspace workspace = getActiveWorkspace(workspaceId);
        WorkspaceMember membership = requireMembership(workspaceId, userId);
        return WorkspaceResponse.of(
                workspace,
                membership.getRole(),
                memberRepository.countByWorkspaceId(workspaceId));
    }

    public List<WorkspaceMemberResponse> getMembers(UUID userId, UUID workspaceId) {
        getActiveWorkspace(workspaceId);
        requireMembership(workspaceId, userId);
        return memberRepository.findMembers(workspaceId, PageRequest.of(0, MAX_MEMBERS)).stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(this::workspaceNotFound);
        if (workspace.getDeletedAt() != null) {
            throw new ApiException(
                    ErrorCode.WORKSPACE_ALREADY_DELETED,
                    "이미 삭제된 워크스페이스입니다.");
        }
        WorkspaceMember membership = requireMembership(workspaceId, userId);
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new ApiException(
                    ErrorCode.WORKSPACE_OWNER_REQUIRED,
                    "워크스페이스 OWNER만 삭제할 수 있습니다.");
        }
        workspace.softDelete(membership.getUser());
        invitationRepository.findAllByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING)
                .forEach(invitation -> invitation.revoke());
    }

    @Transactional
    public WorkspaceWorkContextResponse updateWorkContext(
            UUID userId,
            UUID workspaceId,
            UpdateWorkContextRequest request
    ) {
        getActiveWorkspace(workspaceId);
        validateWorkContext(request);
        WorkspaceMember membership = requireMembership(workspaceId, userId);
        membership.updateWorkContext(
                request.timeZoneId(),
                request.workStart(),
                request.workEnd(),
                request.workDays());
        return WorkspaceWorkContextResponse.from(membership);
    }

    @Transactional
    public void clearWorkContext(UUID userId, UUID workspaceId) {
        getActiveWorkspace(workspaceId);
        WorkspaceMember membership = requireMembership(workspaceId, userId);
        membership.clearWorkContext();
    }

    private Workspace getActiveWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(this::workspaceNotFound);
    }

    private WorkspaceMember requireMembership(UUID workspaceId, UUID userId) {
        return memberRepository.findMembership(workspaceId, userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.WORKSPACE_ACCESS_DENIED,
                        "워크스페이스 멤버만 접근할 수 있습니다."));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void validateWorkContext(UpdateWorkContextRequest request) {
        try {
            ZoneId.of(request.timeZoneId());
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효한 IANA 타임존을 입력해 주세요.");
        }
        if (!request.workStart().isBefore(request.workEnd())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "근무 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    private ApiException workspaceNotFound() {
        return new ApiException(ErrorCode.WORKSPACE_NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
    }
}

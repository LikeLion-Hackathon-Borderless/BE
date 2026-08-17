package com.likelion.asyncalign.user.api;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.dto.UserResponse;
import com.likelion.asyncalign.user.dto.UserSummaryResponse;
import com.likelion.asyncalign.user.dto.UpdateProfileRequest;
import com.likelion.asyncalign.user.dto.UpdateWorkContextRequest;
import com.likelion.asyncalign.user.dto.WorkRoleResponse;
import com.likelion.asyncalign.user.domain.WorkRole;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "사용자", description = "내 정보, 프로필, 근무 컨텍스트, 사용자 검색")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMe(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/me/profile")
    @Operation(summary = "프로필 저장", description = "표시 이름, 역할, 사용자 언어를 저장하고 온보딩 단계를 갱신합니다.")
    UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }

    @PatchMapping("/me/work-context")
    @Operation(summary = "근무 컨텍스트 저장", description = "IANA 타임존, 근무 시작·종료 시각, 근무요일을 저장합니다.")
    UserResponse updateWorkContext(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateWorkContextRequest request
    ) {
        return userService.updateWorkContext(UUID.fromString(jwt.getSubject()), request);
    }

    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드", description = "JPG, PNG, WEBP 파일을 최대 5MB까지 업로드합니다.")
    UserResponse uploadProfileImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {
        return userService.uploadProfileImage(UUID.fromString(jwt.getSubject()), file);
    }

    @GetMapping("/roles")
    @Operation(summary = "역할 목록 조회", description = "프로필에서 선택 가능한 역할 enum과 한글 라벨을 반환합니다.")
    @SecurityRequirements
    List<WorkRoleResponse> getRoles() {
        return java.util.Arrays.stream(WorkRole.values())
                .map(WorkRoleResponse::from)
                .toList();
    }

    @GetMapping
    @Operation(summary = "사용자 검색", description = "이름 또는 이메일로 DM 상대를 검색하며 현재 사용자는 제외합니다.")
    List<UserSummaryResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID workspaceId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(UUID.fromString(jwt.getSubject()), workspaceId, query, size);
    }
}

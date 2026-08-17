package com.likelion.asyncalign.workspace.api;

import com.likelion.asyncalign.global.config.OpenApiConfig;
import com.likelion.asyncalign.user.dto.UpdateWorkContextRequest;
import com.likelion.asyncalign.workspace.application.WorkspaceService;
import com.likelion.asyncalign.workspace.dto.CreateWorkspaceRequest;
import com.likelion.asyncalign.workspace.dto.WorkspaceMemberResponse;
import com.likelion.asyncalign.workspace.dto.WorkspaceResponse;
import com.likelion.asyncalign.workspace.dto.WorkspaceWorkContextResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "워크스페이스", description = "워크스페이스, 멤버, 워크스페이스별 근무 컨텍스트")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "워크스페이스 생성", description = "생성자는 OWNER가 되고 온보딩이 완료됩니다.")
    WorkspaceResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        return workspaceService.create(userId(jwt), request);
    }

    @GetMapping
    @Operation(summary = "내 워크스페이스 목록")
    List<WorkspaceResponse> getMine(@AuthenticationPrincipal Jwt jwt) {
        return workspaceService.getMine(userId(jwt));
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "워크스페이스 상세")
    WorkspaceResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId
    ) {
        return workspaceService.get(userId(jwt), workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "워크스페이스 멤버 목록", description = "표시 이름 오름차순으로 최대 100명을 반환합니다.")
    List<WorkspaceMemberResponse> getMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId
    ) {
        return workspaceService.getMembers(userId(jwt), workspaceId);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "워크스페이스 삭제", description = "OWNER만 워크스페이스를 소프트 삭제할 수 있습니다.")
    void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId
    ) {
        workspaceService.delete(userId(jwt), workspaceId);
    }

    @PutMapping("/{workspaceId}/members/me/work-context")
    @Operation(summary = "워크스페이스 근무 컨텍스트 저장")
    WorkspaceWorkContextResponse updateWorkContext(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkContextRequest request
    ) {
        return workspaceService.updateWorkContext(userId(jwt), workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}/members/me/work-context")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "워크스페이스 근무 컨텍스트 제거", description = "제거 후 계정 기본값을 상속합니다.")
    void clearWorkContext(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId
    ) {
        workspaceService.clearWorkContext(userId(jwt), workspaceId);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

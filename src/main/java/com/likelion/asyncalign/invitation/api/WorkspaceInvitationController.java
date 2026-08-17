package com.likelion.asyncalign.invitation.api;

import com.likelion.asyncalign.global.config.OpenApiConfig;
import com.likelion.asyncalign.invitation.application.WorkspaceInvitationService;
import com.likelion.asyncalign.invitation.dto.CreateEmailInvitationsRequest;
import com.likelion.asyncalign.invitation.dto.CreateInvitationLinkRequest;
import com.likelion.asyncalign.invitation.dto.InvitationBatchResponse;
import com.likelion.asyncalign.invitation.dto.InvitationLinkResponse;
import com.likelion.asyncalign.invitation.dto.InvitationPreviewResponse;
import com.likelion.asyncalign.workspace.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "워크스페이스 초대", description = "이메일 초대, 공유 링크, 초대 미리보기와 수락")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;

    public WorkspaceInvitationController(WorkspaceInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/workspaces/{workspaceId}/invitations")
    @Operation(summary = "이메일 다중 초대")
    InvitationBatchResponse inviteByEmail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateEmailInvitationsRequest request
    ) {
        return invitationService.inviteByEmail(userId(jwt), workspaceId, request);
    }

    @PostMapping("/workspaces/{workspaceId}/invitation-links")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "공유 초대 링크 생성 또는 재발급")
    InvitationLinkResponse createLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateInvitationLinkRequest request
    ) {
        return invitationService.createLink(userId(jwt), workspaceId, request);
    }

    @GetMapping("/workspace-invitations/{token}")
    @SecurityRequirements
    @Operation(summary = "초대 미리보기")
    InvitationPreviewResponse preview(@PathVariable String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/workspace-invitations/{token}/accept")
    @Operation(summary = "초대 수락")
    WorkspaceResponse accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String token
    ) {
        return invitationService.accept(userId(jwt), token);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

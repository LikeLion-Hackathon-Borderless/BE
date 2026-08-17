package com.likelion.asyncalign.invitation.application;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.invitation.domain.InvitationStatus;
import com.likelion.asyncalign.invitation.domain.InvitationType;
import com.likelion.asyncalign.invitation.domain.WorkspaceInvitation;
import com.likelion.asyncalign.invitation.domain.WorkspaceInvitationRepository;
import com.likelion.asyncalign.invitation.dto.CreateEmailInvitationsRequest;
import com.likelion.asyncalign.invitation.dto.CreateInvitationLinkRequest;
import com.likelion.asyncalign.invitation.dto.InvitationBatchResponse;
import com.likelion.asyncalign.invitation.dto.InvitationLinkResponse;
import com.likelion.asyncalign.invitation.dto.InvitationPreviewResponse;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.workspace.domain.Workspace;
import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import com.likelion.asyncalign.workspace.domain.WorkspaceMemberRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRole;
import com.likelion.asyncalign.workspace.dto.WorkspaceResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final InvitationTokenService tokenService;
    private final WorkspaceInvitationMailService mailService;
    private final String frontendBaseUrl;

    public WorkspaceInvitationService(
            WorkspaceInvitationRepository invitationRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            InvitationTokenService tokenService,
            WorkspaceInvitationMailService mailService,
            @Value("${app.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.invitationRepository = invitationRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
    }

    @Transactional
    public InvitationBatchResponse inviteByEmail(
            UUID currentUserId,
            UUID workspaceId,
            CreateEmailInvitationsRequest request
    ) {
        Workspace workspace = getActiveWorkspace(workspaceId);
        WorkspaceMember owner = requireOwner(workspaceId, currentUserId);
        LinkedHashSet<String> emails = request.emails().stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (emails.size() > 20) {
            throw new ApiException(ErrorCode.INVITATION_LIMIT_EXCEEDED, "한 번에 최대 20명까지 초대할 수 있습니다.");
        }

        List<InvitationBatchResponse.Result> results = new ArrayList<>();
        for (String email : emails) {
            User existingUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (existingUser != null && memberRepository.findMembership(workspaceId, existingUser.getId()).isPresent()) {
                results.add(new InvitationBatchResponse.Result(email, "ALREADY_MEMBER", null));
                continue;
            }
            WorkspaceInvitation previous = invitationRepository
                    .findFirstByWorkspaceIdAndInvitedEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
                            workspaceId, email, InvitationStatus.PENDING)
                    .orElse(null);
            if (previous != null && previous.isUsable(Instant.now())) {
                results.add(new InvitationBatchResponse.Result(email, "ALREADY_INVITED", null));
                continue;
            }
            if (previous != null) {
                previous.revoke();
            }

            WorkspaceInvitation invitation = newInvitation(
                    workspace,
                    owner.getUser(),
                    email,
                    InvitationType.EMAIL,
                    7);
            String token = tokenService.generate(invitation.getId());
            invitation.initializeTokenHash(tokenService.hash(token));
            invitationRepository.save(invitation);
            try {
                mailService.send(email, workspace.getName(), owner.getUser().getDisplayName(), token);
                results.add(new InvitationBatchResponse.Result(email, "SENT", null));
            } catch (RuntimeException exception) {
                invitation.revoke();
                results.add(new InvitationBatchResponse.Result(email, "FAILED", "EMAIL_SEND_FAILED"));
            }
        }
        return new InvitationBatchResponse(List.copyOf(results));
    }

    @Transactional
    public InvitationLinkResponse createLink(
            UUID currentUserId,
            UUID workspaceId,
            CreateInvitationLinkRequest request
    ) {
        Workspace workspace = getActiveWorkspace(workspaceId);
        WorkspaceMember owner = requireOwner(workspaceId, currentUserId);
        WorkspaceInvitation active = invitationRepository
                .findFirstByWorkspaceIdAndTypeAndStatusOrderByCreatedAtDesc(
                        workspaceId, InvitationType.LINK, InvitationStatus.PENDING)
                .filter(invitation -> invitation.isUsable(Instant.now()))
                .orElse(null);
        if (active != null && !request.regenerate()) {
            return linkResponse(active, tokenService.generate(active.getId()));
        }
        if (active != null) {
            active.revoke();
        }
        WorkspaceInvitation invitation = newInvitation(
                workspace,
                owner.getUser(),
                null,
                InvitationType.LINK,
                request.effectiveExpiresInDays());
        String token = tokenService.generate(invitation.getId());
        invitation.initializeTokenHash(tokenService.hash(token));
        invitationRepository.save(invitation);
        return linkResponse(invitation, token);
    }

    public InvitationPreviewResponse preview(String rawToken) {
        WorkspaceInvitation invitation = findToken(rawToken);
        validateUsable(invitation);
        return new InvitationPreviewResponse(
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getInviter().getDisplayName(),
                invitation.getInvitedEmail(),
                invitation.getExpiresAt(),
                invitation.getStatus());
    }

    @Transactional
    public WorkspaceResponse accept(UUID currentUserId, String rawToken) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        WorkspaceInvitation invitation = findToken(rawToken);
        Workspace workspace = getActiveWorkspace(invitation.getWorkspace().getId());
        WorkspaceMember existingMembership = memberRepository
                .findMembership(workspace.getId(), currentUserId)
                .orElse(null);
        if (invitation.getStatus() == InvitationStatus.ACCEPTED
                && existingMembership != null
                && invitation.getAcceptedBy() != null
                && invitation.getAcceptedBy().getId().equals(currentUserId)) {
            return response(workspace, existingMembership);
        }
        validateUsable(invitation);
        if (invitation.getInvitedEmail() != null
                && !invitation.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ApiException(
                    ErrorCode.INVITATION_EMAIL_MISMATCH,
                    "초대받은 이메일과 로그인 계정이 일치하지 않습니다.");
        }
        WorkspaceMember membership = existingMembership;
        if (membership == null) {
            membership = memberRepository.save(
                    new WorkspaceMember(workspace, user, WorkspaceRole.MEMBER));
        }
        invitation.accept(user, Instant.now());
        user.completeOnboarding();
        return response(workspace, membership);
    }

    private WorkspaceInvitation newInvitation(
            Workspace workspace,
            User inviter,
            String email,
            InvitationType type,
            int expiresInDays
    ) {
        return new WorkspaceInvitation(
                workspace,
                inviter,
                email,
                type,
                "",
                Instant.now().plus(Duration.ofDays(expiresInDays)));
    }

    private WorkspaceInvitation findToken(String rawToken) {
        if (rawToken == null || !rawToken.startsWith("wsi_")) {
            throw new ApiException(ErrorCode.INVITATION_INVALID, "유효하지 않은 초대 링크입니다.");
        }
        return invitationRepository.findByTokenHashWithDetails(tokenService.hash(rawToken))
                .orElseThrow(() -> new ApiException(
                        ErrorCode.INVITATION_INVALID,
                        "유효하지 않은 초대 링크입니다."));
    }

    private void validateUsable(WorkspaceInvitation invitation) {
        if (invitation.isExpired(Instant.now())) {
            throw new ApiException(ErrorCode.INVITATION_EXPIRED, "초대 링크가 만료되었습니다.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(ErrorCode.INVITATION_INVALID, "이미 사용되었거나 취소된 초대입니다.");
        }
    }

    private Workspace getActiveWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        "워크스페이스를 찾을 수 없습니다."));
    }

    private WorkspaceMember requireOwner(UUID workspaceId, UUID userId) {
        WorkspaceMember membership = memberRepository.findMembership(workspaceId, userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.WORKSPACE_ACCESS_DENIED,
                        "워크스페이스 멤버만 접근할 수 있습니다."));
        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new ApiException(
                    ErrorCode.WORKSPACE_OWNER_REQUIRED,
                    "워크스페이스 OWNER만 초대할 수 있습니다.");
        }
        return membership;
    }

    private InvitationLinkResponse linkResponse(WorkspaceInvitation invitation, String token) {
        return new InvitationLinkResponse(
                token,
                frontendBaseUrl + "/invitations/" + token,
                invitation.getExpiresAt());
    }

    private WorkspaceResponse response(Workspace workspace, WorkspaceMember membership) {
        return WorkspaceResponse.of(
                workspace,
                membership.getRole(),
                memberRepository.countByWorkspaceId(workspace.getId()));
    }
}

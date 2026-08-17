package com.likelion.asyncalign.alignment.application;

import com.likelion.asyncalign.alignment.domain.AgreementLog;
import com.likelion.asyncalign.alignment.domain.AgreementLogFileReference;
import com.likelion.asyncalign.alignment.domain.AgreementLogFileReferenceRepository;
import com.likelion.asyncalign.alignment.domain.AgreementLogRepository;
import com.likelion.asyncalign.alignment.domain.AgreementStatus;
import com.likelion.asyncalign.alignment.domain.CardResponseType;
import com.likelion.asyncalign.alignment.domain.UnderstandingCard;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRepository;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardResponseRepository;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRevision;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRevisionRepository;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardState;
import com.likelion.asyncalign.alignment.dto.AiReviewResponse;
import com.likelion.asyncalign.alignment.dto.CardResponseRequest;
import com.likelion.asyncalign.alignment.dto.CreateCardRevisionRequest;
import com.likelion.asyncalign.alignment.dto.CreateUnderstandingCardRequest;
import com.likelion.asyncalign.alignment.dto.UnderstandingCardResponse;
import com.likelion.asyncalign.attachment.application.AttachmentService;
import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.messenger.application.ConversationService;
import com.likelion.asyncalign.messenger.domain.ConfirmationStatus;
import com.likelion.asyncalign.messenger.domain.ConversationMember;
import com.likelion.asyncalign.messenger.domain.ConversationMemberRepository;
import com.likelion.asyncalign.messenger.domain.DeliveryMode;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.messenger.domain.MessageRepository;
import com.likelion.asyncalign.user.domain.User;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnderstandingCardService {

    private static final int MAX_REVISIONS = 5;

    private final UnderstandingCardRepository cardRepository;
    private final UnderstandingCardRevisionRepository revisionRepository;
    private final UnderstandingCardResponseRepository responseRepository;
    private final AgreementLogRepository agreementLogRepository;
    private final AgreementLogFileReferenceRepository fileReferenceRepository;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final AttachmentService attachmentService;
    private final AiReviewAnalyzer analyzer;
    private final AiReviewService aiReviewService;

    public UnderstandingCardService(
            UnderstandingCardRepository cardRepository,
            UnderstandingCardRevisionRepository revisionRepository,
            UnderstandingCardResponseRepository responseRepository,
            AgreementLogRepository agreementLogRepository,
            AgreementLogFileReferenceRepository fileReferenceRepository,
            MessageRepository messageRepository,
            ConversationService conversationService,
            ConversationMemberRepository conversationMemberRepository,
            AttachmentService attachmentService,
            AiReviewAnalyzer analyzer,
            AiReviewService aiReviewService
    ) {
        this.cardRepository = cardRepository;
        this.revisionRepository = revisionRepository;
        this.responseRepository = responseRepository;
        this.agreementLogRepository = agreementLogRepository;
        this.fileReferenceRepository = fileReferenceRepository;
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.conversationMemberRepository = conversationMemberRepository;
        this.attachmentService = attachmentService;
        this.analyzer = analyzer;
        this.aiReviewService = aiReviewService;
    }

    @Transactional
    public CreateResult createForMessage(
            UUID messageId,
            UUID userId,
            CreateUnderstandingCardRequest request
    ) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(ErrorCode.MESSAGE_NOT_FOUND, "메시지를 찾을 수 없습니다."));
        conversationService.getMembership(message.getConversation().getId(), userId);
        if (message.getSender().getId().equals(userId)) {
            throw new ApiException(
                    ErrorCode.CARD_RESPONSE_NOT_ALLOWED,
                    "메시지를 받은 사용자만 이해 돕기 카드를 만들 수 있습니다.");
        }
        UnderstandingCard existing = cardRepository.findByMessageId(messageId).orElse(null);
        if (existing != null) {
            return new CreateResult(toResponse(existing, userId), false);
        }
        if (message.getDeliveryMode() != DeliveryMode.AS_IS) {
            throw new ApiException(ErrorCode.CARD_INVALID_STATE, "AI 확정 메시지에는 이미 공통 이해 카드가 있습니다.");
        }
        User recipient = user(message.getConversation().getId(), userId);
        List<String> attachmentContexts = attachmentService.getMessageAttachmentEntities(messageId).stream()
                .map(attachment -> attachment.getOriginalFileName() + ": "
                        + (attachment.getExtractedText() == null
                                ? "텍스트 추출 없음"
                                : attachment.getExtractedText()))
                .toList();
        List<String> context = request.includeConversationContext()
                ? messageRepository.findRecentForContext(
                                message.getConversation().getId(), PageRequest.of(0, 10)).stream()
                        .map(Message::getContent)
                        .toList()
                : List.of();
        AiReviewAnalyzer.Analysis analysis = analyzer.analyze(new AiReviewAnalyzer.AnalysisInput(
                message.getContent(),
                message.getSender().getPreferredLanguage(),
                recipient.getPreferredLanguage(),
                recipient.getId(),
                context,
                attachmentContexts));
        boolean needsClarification = analysis.task() == null
                || analysis.deadline() == null
                || analysis.expectedOutcome() == null;
        UnderstandingCard card = cardRepository.save(new UnderstandingCard(
                message,
                null,
                message.getSender(),
                recipient,
                analysis.task(),
                recipient,
                analysis.deadline(),
                analysis.expectedOutcome(),
                analysis.translatedContent(),
                needsClarification));
        revisionRepository.save(new UnderstandingCardRevision(
                card,
                1,
                card.getTask(),
                card.getAssignee(),
                card.getDeadline(),
                card.getExpectedOutcome(),
                "수신자 이해 돕기 생성",
                recipient));
        message.updateConfirmationStatus(ConfirmationStatus.REVIEW);
        return new CreateResult(toResponse(card, userId), true);
    }

    public UnderstandingCardResponse get(UUID cardId, UUID userId) {
        UnderstandingCard card = getAuthorized(cardId, userId);
        return toResponse(card, userId);
    }

    @Transactional
    public UnderstandingCardResponse respond(UUID cardId, UUID userId, CardResponseRequest request) {
        UnderstandingCard card = getAuthorized(cardId, userId);
        if (!card.getRecipient().getId().equals(userId)) {
            throw new ApiException(ErrorCode.CARD_RESPONSE_NOT_ALLOWED, "수신자만 카드에 응답할 수 있습니다.");
        }
        if (card.getState() != UnderstandingCardState.REVIEW
                || responseRepository.existsByCardIdAndRevisionNumber(cardId, card.getRevisionNumber())) {
            throw new ApiException(ErrorCode.CARD_INVALID_STATE, "현재 카드 revision에는 응답할 수 없습니다.");
        }
        validateResponse(request);
        com.likelion.asyncalign.alignment.domain.UnderstandingCardResponse response =
                responseRepository.save(new com.likelion.asyncalign.alignment.domain.UnderstandingCardResponse(
                        card,
                        card.getRevisionNumber(),
                        card.getRecipient(),
                        request.type(),
                        normalize(request.comment()),
                        request.proposedDeadline()));
        if (request.type() == CardResponseType.AGREE) {
            card.agree();
            card.getMessage().updateConfirmationStatus(ConfirmationStatus.AGREED);
            createAgreementLog(card, AgreementStatus.AGREED, card.getRecipient(), Instant.now());
        } else {
            card.requestChange();
            card.getMessage().updateConfirmationStatus(ConfirmationStatus.PENDING);
            createAgreementLog(card, AgreementStatus.PENDING, null, null);
        }
        return toResponse(card, userId);
    }

    @Transactional
    public UnderstandingCardResponse revise(
            UUID cardId,
            UUID userId,
            CreateCardRevisionRequest request
    ) {
        UnderstandingCard card = getAuthorized(cardId, userId);
        if (!card.getSender().getId().equals(userId)) {
            throw new ApiException(ErrorCode.CARD_RESPONSE_NOT_ALLOWED, "발신자만 카드 수정본을 제출할 수 있습니다.");
        }
        if (card.getState() != UnderstandingCardState.PENDING) {
            throw new ApiException(ErrorCode.CARD_INVALID_STATE, "조정 요청을 받은 카드만 수정할 수 있습니다.");
        }
        if (card.getRevisionNumber() >= MAX_REVISIONS) {
            throw new ApiException(
                    ErrorCode.REVISION_LIMIT_EXCEEDED,
                    "수정 횟수를 초과했습니다. 직접 대화나 회의를 권장합니다.");
        }
        card.revise(request.task().trim(), request.deadline(), request.expectedOutcome().trim());
        revisionRepository.save(new UnderstandingCardRevision(
                card,
                card.getRevisionNumber(),
                card.getTask(),
                card.getAssignee(),
                card.getDeadline(),
                card.getExpectedOutcome(),
                request.changeNote().trim(),
                card.getSender()));
        card.getMessage().updateConfirmationStatus(ConfirmationStatus.REVIEW);
        return toResponse(card, userId);
    }

    public UnderstandingCardResponse toResponse(UnderstandingCard card, UUID viewerId) {
        User viewer = user(card.getConversation().getId(), viewerId);
        com.likelion.asyncalign.alignment.domain.UnderstandingCardResponse latest = responseRepository
                .findFirstByCardIdOrderByCreatedAtDesc(card.getId())
                .orElse(null);
        UnderstandingCardResponse.Assignee assignee = card.getAssignee() == null
                ? null
                : new UnderstandingCardResponse.Assignee(
                        card.getAssignee().getId(), card.getAssignee().getDisplayName());
        UnderstandingCardResponse.Deadline deadline = card.getDeadline() == null
                ? null
                : new UnderstandingCardResponse.Deadline(
                        card.getDeadline(),
                        card.getDeadline().atZone(ZoneId.of(viewer.getTimeZoneId())),
                        viewer.getTimeZoneId());
        List<AiReviewResponse.Evidence> evidence = card.getAiReview() == null
                ? List.of()
                : aiReviewService.evidence(card.getAiReview().getId());
        return new UnderstandingCardResponse(
                card.getId(),
                card.getMessage().getId(),
                card.getState(),
                card.getRevisionNumber(),
                card.getTask(),
                assignee,
                deadline,
                card.getExpectedOutcome(),
                card.getOriginalContent(),
                card.getTranslatedContent(),
                card.isNeedsClarification(),
                attachmentService.getMessageAttachments(card.getMessage().getId()),
                evidence,
                latest == null ? null : new UnderstandingCardResponse.LatestResponse(
                        latest.getId(),
                        latest.getRevisionNumber(),
                        latest.getType(),
                        latest.getComment(),
                        latest.getProposedDeadline(),
                        latest.getResponder().getId(),
                        latest.getCreatedAt()),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }

    private void validateResponse(CardResponseRequest request) {
        if (request.type() == CardResponseType.REQUEST_DEADLINE_CHANGE
                && request.proposedDeadline() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "기한 조정 요청에는 제안 기한이 필요합니다.");
        }
        if (request.type() == CardResponseType.REQUEST_CLARIFICATION
                && normalize(request.comment()) == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "설명 요청 내용을 입력해 주세요.");
        }
    }

    private void createAgreementLog(
            UnderstandingCard card,
            AgreementStatus status,
            User agreedBy,
            Instant agreedAt
    ) {
        AgreementLog log = agreementLogRepository.save(new AgreementLog(card, status, agreedBy, agreedAt));
        List<AiReviewResponse.Evidence> evidence = card.getAiReview() == null
                ? List.of()
                : aiReviewService.evidence(card.getAiReview().getId());
        for (Attachment attachment : attachmentService.getMessageAttachmentEntities(card.getMessage().getId())) {
            String locator = evidence.stream()
                    .filter(item -> item.attachmentId().equals(attachment.getId()))
                    .map(AiReviewResponse.Evidence::locator)
                    .findFirst()
                    .orElse("원본 파일");
            fileReferenceRepository.save(new AgreementLogFileReference(log, attachment, locator));
        }
    }

    private UnderstandingCard getAuthorized(UUID cardId, UUID userId) {
        UnderstandingCard card = cardRepository.findWithDetailsById(cardId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.UNDERSTANDING_CARD_NOT_FOUND,
                        "공통 이해 카드를 찾을 수 없습니다."));
        conversationService.getMembership(card.getConversation().getId(), userId);
        return card;
    }

    private User user(UUID conversationId, UUID userId) {
        return conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .map(ConversationMember::getUser)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "대화에 접근할 수 없습니다."));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateResult(UnderstandingCardResponse response, boolean created) {
    }

}

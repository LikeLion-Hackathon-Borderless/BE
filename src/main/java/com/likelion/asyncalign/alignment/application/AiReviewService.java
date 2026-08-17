package com.likelion.asyncalign.alignment.application;

import com.likelion.asyncalign.alignment.domain.AiReview;
import com.likelion.asyncalign.alignment.domain.AiReviewEvidence;
import com.likelion.asyncalign.alignment.domain.AiReviewEvidenceRepository;
import com.likelion.asyncalign.alignment.domain.AiReviewRepository;
import com.likelion.asyncalign.alignment.domain.AiReviewStatus;
import com.likelion.asyncalign.alignment.domain.ConfidenceLevel;
import com.likelion.asyncalign.alignment.domain.UnderstandingCard;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRepository;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRevision;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardRevisionRepository;
import com.likelion.asyncalign.alignment.dto.AiReviewResponse;
import com.likelion.asyncalign.alignment.dto.CreateAiReviewRequest;
import com.likelion.asyncalign.alignment.dto.SendAiReviewRequest;
import com.likelion.asyncalign.alignment.dto.UpdateAiReviewRequest;
import com.likelion.asyncalign.attachment.application.AttachmentService;
import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.attachment.domain.AttachmentProcessingStatus;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.messenger.application.ConversationService;
import com.likelion.asyncalign.messenger.application.MessageService;
import com.likelion.asyncalign.messenger.domain.ConversationMember;
import com.likelion.asyncalign.messenger.domain.ConversationMemberRepository;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.messenger.domain.MessageRepository;
import com.likelion.asyncalign.messenger.dto.MessageResponse;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import com.likelion.asyncalign.workspace.domain.WorkspaceMemberRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiReviewService {

    private final AiReviewRepository reviewRepository;
    private final AiReviewEvidenceRepository evidenceRepository;
    private final ConversationService conversationService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final AttachmentService attachmentService;
    private final AiReviewAnalyzer analyzer;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MessageService messageService;
    private final UnderstandingCardRepository cardRepository;
    private final UnderstandingCardRevisionRepository revisionRepository;

    public AiReviewService(
            AiReviewRepository reviewRepository,
            AiReviewEvidenceRepository evidenceRepository,
            ConversationService conversationService,
            ConversationMemberRepository conversationMemberRepository,
            MessageRepository messageRepository,
            AttachmentService attachmentService,
            AiReviewAnalyzer analyzer,
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            MessageService messageService,
            UnderstandingCardRepository cardRepository,
            UnderstandingCardRevisionRepository revisionRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.evidenceRepository = evidenceRepository;
        this.conversationService = conversationService;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.attachmentService = attachmentService;
        this.analyzer = analyzer;
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.messageService = messageService;
        this.cardRepository = cardRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional
    public AiReviewResponse create(UUID conversationId, UUID userId, CreateAiReviewRequest request) {
        ConversationMember membership = conversationService.getMembership(conversationId, userId);
        User recipient = otherParticipant(conversationId, userId);
        List<Attachment> attachments = attachmentService.getAttachmentsForReview(
                membership.getConversation(), userId, request.safeAttachmentIds());
        List<String> recentMessages = messageRepository.findRecentForContext(
                        conversationId, PageRequest.of(0, 10)).stream()
                .map(Message::getContent)
                .toList();
        List<String> attachmentContexts = attachments.stream()
                .map(attachment -> attachment.getOriginalFileName() + ": "
                        + (attachment.getExtractedText() == null ? "텍스트 추출 없음" : attachment.getExtractedText()))
                .toList();
        AiReviewAnalyzer.Analysis analysis = analyzer.analyze(new AiReviewAnalyzer.AnalysisInput(
                request.content().trim(),
                membership.getUser().getPreferredLanguage(),
                recipient.getPreferredLanguage(),
                recipient.getId(),
                recentMessages,
                attachmentContexts));
        AiReview review = reviewRepository.save(new AiReview(
                membership.getConversation(),
                membership.getUser(),
                request.content().trim(),
                analysis.sourceLanguage(),
                recipient.getPreferredLanguage(),
                analysis.translatedContent(),
                analysis.task(),
                analysis.taskConfidence(),
                analysis.deadline(),
                analysis.deadlineConfidence(),
                analysis.expectedOutcome(),
                analysis.expectedOutcomeConfidence(),
                analysis.provider(),
                Instant.now().plus(Duration.ofHours(24)),
                attachments));
        for (Attachment attachment : attachments) {
            String excerpt = attachment.getExtractedText();
            if (excerpt != null && excerpt.length() > 500) {
                excerpt = excerpt.substring(0, 500);
            }
            evidenceRepository.save(new AiReviewEvidence(
                    review,
                    attachment,
                    excerpt == null ? "원본 파일" : "추출 텍스트",
                    excerpt,
                    excerpt == null ? ConfidenceLevel.LOW : ConfidenceLevel.MEDIUM));
        }
        return toResponse(review, recipient);
    }

    @Transactional
    public AiReviewResponse get(UUID reviewId, UUID userId) {
        AiReview review = getAuthorized(reviewId, userId);
        expire(review);
        return toResponse(review, otherParticipant(review.getConversation().getId(), review.getCreator().getId()));
    }

    @Transactional
    public AiReviewResponse update(UUID reviewId, UUID userId, UpdateAiReviewRequest request) {
        AiReview review = getAuthorized(reviewId, userId);
        requireCreator(review, userId);
        expire(review);
        if (review.getStatus() == AiReviewStatus.EXPIRED) {
            throw new ApiException(ErrorCode.AI_REVIEW_EXPIRED, "AI 검토가 만료되었습니다.");
        }
        if (review.getStatus() == AiReviewStatus.SENT) {
            throw new ApiException(ErrorCode.AI_REVIEW_NOT_CONFIRMED, "이미 전송한 AI 검토는 수정할 수 없습니다.");
        }
        User recipient = otherParticipant(review.getConversation().getId(), userId);
        User assignee = request.assigneeUserId() == null
                ? null
                : userRepository.findById(request.assigneeUserId()).orElseThrow(() ->
                        new ApiException(ErrorCode.USER_NOT_FOUND, "담당자를 찾을 수 없습니다."));
        if (assignee != null && !assignee.getId().equals(recipient.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "1:1 대화의 수신자만 담당자로 확정할 수 있습니다.");
        }
        String task = normalize(request.task());
        String expectedOutcome = normalize(request.expectedOutcome());
        if (request.confirmed()
                && (task == null || assignee == null || request.deadline() == null || expectedOutcome == null)) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "확정하려면 업무, 담당자, 정확한 기한, 기대 결과를 모두 입력해야 합니다.");
        }
        confirmEvidence(reviewId, request.safeConfirmedEvidenceIds());
        review.updateFinal(task, assignee, request.deadline(), expectedOutcome, request.confirmed());
        return toResponse(review, recipient);
    }

    @Transactional
    public MessageResponse send(UUID reviewId, UUID userId, SendAiReviewRequest request) {
        AiReview review = getAuthorized(reviewId, userId);
        requireCreator(review, userId);
        expire(review);
        if (review.getStatus() != AiReviewStatus.CONFIRMED) {
            throw new ApiException(
                    ErrorCode.AI_REVIEW_NOT_CONFIRMED,
                    "AI 검토의 업무 조건을 먼저 확정해야 합니다.");
        }
        ConversationMember membership = conversationService.getMembership(
                review.getConversation().getId(), userId);
        User recipient = otherParticipant(review.getConversation().getId(), userId);
        String content = normalize(request.content());
        if (content == null) {
            content = review.getOriginalContent();
        }
        Message message = messageService.createAiConfirmedMessage(
                membership, review, content, request.scheduledFor());
        UnderstandingCard card = cardRepository.save(new UnderstandingCard(
                message,
                review,
                membership.getUser(),
                recipient,
                review.getFinalTask(),
                review.getFinalAssignee(),
                review.getFinalDeadline(),
                review.getFinalExpectedOutcome(),
                review.getTranslatedContent(),
                false));
        revisionRepository.save(new UnderstandingCardRevision(
                card,
                1,
                card.getTask(),
                card.getAssignee(),
                card.getDeadline(),
                card.getExpectedOutcome(),
                "AI 검토 확정 전송",
                membership.getUser()));
        review.markSent();
        return messageService.toResponse(message, membership.getUser());
    }

    public List<AiReviewResponse.Evidence> evidence(UUID reviewId) {
        return evidenceRepository.findAllByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(item -> new AiReviewResponse.Evidence(
                        item.getId(),
                        item.getAttachment().getId(),
                        item.getAttachment().getOriginalFileName(),
                        item.getLocator(),
                        item.getExcerpt(),
                        item.getConfidence(),
                        item.isConfirmed()))
                .toList();
    }

    private AiReviewResponse toResponse(AiReview review, User recipient) {
        boolean confirmed = review.getStatus() == AiReviewStatus.CONFIRMED
                || review.getStatus() == AiReviewStatus.SENT;
        String task = review.getFinalTask() != null ? review.getFinalTask() : review.getAiTask();
        UUID assigneeId = review.getFinalAssignee() == null ? recipient.getId() : review.getFinalAssignee().getId();
        Instant deadline = review.getFinalDeadline() != null ? review.getFinalDeadline() : review.getAiDeadline();
        String outcome = review.getFinalExpectedOutcome() != null
                ? review.getFinalExpectedOutcome()
                : review.getAiExpectedOutcome();
        ConfidenceLevel finalConfidence = confirmed ? ConfidenceLevel.HIGH : null;
        ZonedDateTime senderLocal = deadline == null ? null : deadline.atZone(ZoneId.of(review.getCreator().getTimeZoneId()));
        ZonedDateTime recipientLocal = deadline == null ? null : deadline.atZone(ZoneId.of(recipient.getTimeZoneId()));
        return new AiReviewResponse(
                review.getId(),
                review.getConversation().getId(),
                review.getStatus(),
                review.getOriginalContent(),
                review.getSourceLanguage(),
                review.getRecipientLanguage(),
                review.getTranslatedContent(),
                new AiReviewResponse.StructuredFields(
                        new AiReviewResponse.TextField(
                                task,
                                finalConfidence == null ? review.getAiTaskConfidence() : finalConfidence,
                                confirmed),
                        new AiReviewResponse.AssigneeField(
                                assigneeId,
                                confirmed ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM,
                                confirmed),
                        new AiReviewResponse.DeadlineField(
                                deadline,
                                senderLocal,
                                recipientLocal,
                                finalConfidence == null ? review.getAiDeadlineConfidence() : finalConfidence,
                                confirmed),
                        new AiReviewResponse.TextField(
                                outcome,
                                finalConfidence == null ? review.getAiExpectedOutcomeConfidence() : finalConfidence,
                                confirmed)),
                evidence(review.getId()),
                warnings(review, recipient, deadline),
                review.getProvider(),
                review.getCreatedAt(),
                review.getExpiresAt());
    }

    private List<AiReviewResponse.Warning> warnings(AiReview review, User recipient, Instant deadline) {
        List<AiReviewResponse.Warning> warnings = new ArrayList<>();
        if (deadline == null) {
            warnings.add(new AiReviewResponse.Warning(
                    "AMBIGUOUS_DEADLINE",
                    "정확한 날짜·시각·타임존을 발신자가 확인해야 합니다.",
                    null));
        } else {
            WorkspaceMember context = workspaceMemberRepository.findMembership(
                    review.getConversation().getWorkspace().getId(), recipient.getId()).orElseThrow();
            if (outsideWorkHours(context, deadline)) {
                warnings.add(new AiReviewResponse.Warning(
                        "OUTSIDE_RECIPIENT_WORK_HOURS",
                        "확정 기한이 수신자의 근무시간 밖입니다.",
                        suggestNextWorkStart(context, deadline)));
            }
        }
        if (review.getFinalExpectedOutcome() == null && review.getAiExpectedOutcome() == null) {
            warnings.add(new AiReviewResponse.Warning(
                    "AMBIGUOUS_EXPECTED_OUTCOME",
                    "기대 결과 또는 완료 기준을 확인해 주세요.",
                    null));
        }
        if (review.getProvider().contains("AI_FAILURE")) {
            warnings.add(new AiReviewResponse.Warning(
                    "AI_REVIEW_FAILED",
                    "AI 호출에 실패해 안전한 로컬 분석 결과를 표시합니다.",
                    null));
        }
        for (Attachment attachment : review.getAttachments()) {
            if (attachment.getProcessingStatus() == AttachmentProcessingStatus.EXTRACTION_FAILED) {
                warnings.add(new AiReviewResponse.Warning(
                        "ATTACHMENT_EXTRACTION_FAILED",
                        attachment.getOriginalFileName() + "의 텍스트를 추출하지 못했습니다. 원본을 확인해 주세요.",
                        null));
            }
        }
        return List.copyOf(warnings);
    }

    private boolean outsideWorkHours(WorkspaceMember context, Instant deadline) {
        ZonedDateTime local = deadline.atZone(ZoneId.of(context.getEffectiveTimeZoneId()));
        return !context.getEffectiveWorkDays().contains(local.getDayOfWeek())
                || local.toLocalTime().isBefore(context.getEffectiveWorkStart())
                || local.toLocalTime().isAfter(context.getEffectiveWorkEnd());
    }

    private Instant suggestNextWorkStart(WorkspaceMember context, Instant deadline) {
        ZoneId zone = ZoneId.of(context.getEffectiveTimeZoneId());
        ZonedDateTime local = deadline.atZone(zone);
        LocalDate date = local.toLocalDate();
        LocalTime start = context.getEffectiveWorkStart();
        if (context.getEffectiveWorkDays().contains(date.getDayOfWeek())
                && local.toLocalTime().isBefore(start)) {
            return date.atTime(start).atZone(zone).toInstant();
        }
        do {
            date = date.plusDays(1);
        } while (!context.getEffectiveWorkDays().contains(date.getDayOfWeek()));
        return date.atTime(start).atZone(zone).toInstant();
    }

    private void confirmEvidence(UUID reviewId, List<UUID> evidenceIds) {
        Set<UUID> requested = new HashSet<>(evidenceIds);
        List<AiReviewEvidence> all = evidenceRepository.findAllByReviewIdOrderByCreatedAtAsc(reviewId);
        Set<UUID> available = all.stream().map(AiReviewEvidence::getId).collect(java.util.stream.Collectors.toSet());
        if (!available.containsAll(requested)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이 AI 검토에 속하지 않는 근거가 포함되어 있습니다.");
        }
        all.stream().filter(item -> requested.contains(item.getId())).forEach(AiReviewEvidence::confirm);
    }

    private AiReview getAuthorized(UUID reviewId, UUID userId) {
        AiReview review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_REVIEW_NOT_FOUND, "AI 검토를 찾을 수 없습니다."));
        conversationService.getMembership(review.getConversation().getId(), userId);
        return review;
    }

    private void requireCreator(AiReview review, UUID userId) {
        if (!review.getCreator().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "AI 검토를 만든 사용자만 수정하거나 전송할 수 있습니다.");
        }
    }

    private void expire(AiReview review) {
        review.expireIfNecessary(Instant.now());
    }

    private User otherParticipant(UUID conversationId, UUID userId) {
        return conversationMemberRepository.findAllWithUserByConversationId(conversationId).stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "대화 상대를 찾을 수 없습니다."));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

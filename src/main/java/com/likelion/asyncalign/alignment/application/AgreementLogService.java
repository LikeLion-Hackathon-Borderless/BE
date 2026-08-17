package com.likelion.asyncalign.alignment.application;

import com.likelion.asyncalign.alignment.domain.AgreementLog;
import com.likelion.asyncalign.alignment.domain.AgreementLogFileReferenceRepository;
import com.likelion.asyncalign.alignment.domain.AgreementLogRepository;
import com.likelion.asyncalign.alignment.dto.AgreementLogPageResponse;
import com.likelion.asyncalign.messenger.application.ConversationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AgreementLogService {

    private final AgreementLogRepository agreementLogRepository;
    private final AgreementLogFileReferenceRepository fileReferenceRepository;
    private final ConversationService conversationService;

    public AgreementLogService(
            AgreementLogRepository agreementLogRepository,
            AgreementLogFileReferenceRepository fileReferenceRepository,
            ConversationService conversationService
    ) {
        this.agreementLogRepository = agreementLogRepository;
        this.fileReferenceRepository = fileReferenceRepository;
        this.conversationService = conversationService;
    }

    public AgreementLogPageResponse getLogs(
            UUID conversationId,
            UUID userId,
            Instant before,
            int size
    ) {
        conversationService.getMembership(conversationId, userId);
        int safeSize = Math.clamp(size, 1, 100);
        Instant cursor = before == null ? Instant.now().plusSeconds(1) : before;
        List<AgreementLog> result = agreementLogRepository.findPageBefore(
                conversationId, cursor, PageRequest.of(0, safeSize + 1));
        boolean hasMore = result.size() > safeSize;
        List<AgreementLog> page = new ArrayList<>(result.subList(0, Math.min(result.size(), safeSize)));
        Instant nextBefore = hasMore && !page.isEmpty() ? page.getLast().getCreatedAt() : null;
        Collections.reverse(page);
        return new AgreementLogPageResponse(
                page.stream().map(this::toResponse).toList(),
                hasMore,
                nextBefore);
    }

    private AgreementLogPageResponse.Log toResponse(AgreementLog log) {
        AgreementLogPageResponse.AgreedBy agreedBy = log.getAgreedBy() == null
                ? null
                : new AgreementLogPageResponse.AgreedBy(
                        log.getAgreedBy().getId(), log.getAgreedBy().getDisplayName());
        List<AgreementLogPageResponse.FileReference> files = fileReferenceRepository
                .findAllByAgreementLogId(log.getId()).stream()
                .map(file -> new AgreementLogPageResponse.FileReference(
                        file.getAttachmentId(), file.getFileName(), file.getLocator()))
                .toList();
        return new AgreementLogPageResponse.Log(
                log.getId(),
                log.getCard().getId(),
                log.getRevisionNumber(),
                log.getStatus(),
                log.getTask(),
                log.getDeadline(),
                log.getExpectedOutcome(),
                agreedBy,
                log.getAgreedAt(),
                files,
                log.getCreatedAt());
    }
}

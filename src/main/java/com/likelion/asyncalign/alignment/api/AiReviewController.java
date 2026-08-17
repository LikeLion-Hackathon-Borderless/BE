package com.likelion.asyncalign.alignment.api;

import com.likelion.asyncalign.alignment.application.AiReviewService;
import com.likelion.asyncalign.alignment.dto.AiReviewResponse;
import com.likelion.asyncalign.alignment.dto.CreateAiReviewRequest;
import com.likelion.asyncalign.alignment.dto.SendAiReviewRequest;
import com.likelion.asyncalign.alignment.dto.UpdateAiReviewRequest;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import com.likelion.asyncalign.messenger.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "AI 검토", description = "업무 조건 추출, 사용자 확정, 공통 이해 카드 전송")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiReviewController {

    private final AiReviewService aiReviewService;

    public AiReviewController(AiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/conversations/{conversationId}/ai-reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "AI 검토 생성")
    AiReviewResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @Valid @RequestBody CreateAiReviewRequest request
    ) {
        return aiReviewService.create(conversationId, userId(jwt), request);
    }

    @GetMapping("/ai-reviews/{reviewId}")
    @Operation(summary = "AI 검토 조회")
    AiReviewResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reviewId
    ) {
        return aiReviewService.get(reviewId, userId(jwt));
    }

    @PatchMapping("/ai-reviews/{reviewId}")
    @Operation(summary = "AI 검토 수정·확정")
    AiReviewResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateAiReviewRequest request
    ) {
        return aiReviewService.update(reviewId, userId(jwt), request);
    }

    @PostMapping("/ai-reviews/{reviewId}/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "AI 검토 확정 메시지 전송")
    MessageResponse send(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reviewId,
            @Valid @RequestBody SendAiReviewRequest request
    ) {
        return aiReviewService.send(reviewId, userId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

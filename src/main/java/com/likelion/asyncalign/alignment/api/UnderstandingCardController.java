package com.likelion.asyncalign.alignment.api;

import com.likelion.asyncalign.alignment.application.UnderstandingCardService;
import com.likelion.asyncalign.alignment.dto.CardResponseRequest;
import com.likelion.asyncalign.alignment.dto.CreateCardRevisionRequest;
import com.likelion.asyncalign.alignment.dto.CreateUnderstandingCardRequest;
import com.likelion.asyncalign.alignment.dto.UnderstandingCardResponse;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "공통 이해 카드", description = "이해 돕기, 수신자 응답, 발신자 수정본")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UnderstandingCardController {

    private final UnderstandingCardService cardService;

    public UnderstandingCardController(UnderstandingCardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/messages/{messageId}/understanding-cards")
    @Operation(summary = "일반 메시지 이해 돕기 카드 생성")
    ResponseEntity<UnderstandingCardResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID messageId,
            @RequestBody CreateUnderstandingCardRequest request
    ) {
        UnderstandingCardService.CreateResult result = cardService.createForMessage(
                messageId, userId(jwt), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.response());
    }

    @GetMapping("/understanding-cards/{cardId}")
    @Operation(summary = "공통 이해 카드 조회")
    UnderstandingCardResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cardId
    ) {
        return cardService.get(cardId, userId(jwt));
    }

    @PostMapping("/understanding-cards/{cardId}/responses")
    @Operation(summary = "수신자 카드 응답")
    UnderstandingCardResponse respond(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cardId,
            @Valid @RequestBody CardResponseRequest request
    ) {
        return cardService.respond(cardId, userId(jwt), request);
    }

    @PostMapping("/understanding-cards/{cardId}/revisions")
    @Operation(summary = "발신자 카드 수정본 제출")
    UnderstandingCardResponse revise(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreateCardRevisionRequest request
    ) {
        return cardService.revise(cardId, userId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

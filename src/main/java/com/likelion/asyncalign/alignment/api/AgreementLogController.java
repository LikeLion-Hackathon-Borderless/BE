package com.likelion.asyncalign.alignment.api;

import com.likelion.asyncalign.alignment.application.AgreementLogService;
import com.likelion.asyncalign.alignment.dto.AgreementLogPageResponse;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/agreement-logs")
@Tag(name = "합의 기록", description = "대화별 합의·조정 이력")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AgreementLogController {

    private final AgreementLogService agreementLogService;

    public AgreementLogController(AgreementLogService agreementLogService) {
        this.agreementLogService = agreementLogService;
    }

    @GetMapping
    @Operation(summary = "대화별 합의 기록 조회")
    AgreementLogPageResponse getLogs(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int size
    ) {
        return agreementLogService.getLogs(
                conversationId,
                UUID.fromString(jwt.getSubject()),
                before,
                size);
    }
}

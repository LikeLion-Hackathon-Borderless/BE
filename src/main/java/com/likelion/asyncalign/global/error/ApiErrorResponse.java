package com.likelion.asyncalign.global.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "공통 API 오류 응답")
public record ApiErrorResponse(
        @Schema(example = "2026-08-14T09:00:00Z")
        Instant timestamp,
        @Schema(example = "400")
        int status,
        @Schema(example = "INVALID_REQUEST")
        String code,
        @Schema(example = "요청 값이 올바르지 않습니다.")
        String message,
        @Schema(description = "필드별 검증 오류. 없으면 빈 객체")
        Map<String, String> fieldErrors
) {
}

package com.likelion.asyncalign.messenger.api;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.messenger.application.MessageService;
import com.likelion.asyncalign.messenger.dto.MessagePageResponse;
import com.likelion.asyncalign.messenger.dto.MessageResponse;
import com.likelion.asyncalign.messenger.dto.SendMessageRequest;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@Tag(name = "메시지", description = "1:1 대화 메시지 조회와 전송")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "메시지 목록 조회", description = "before cursor 이전 메시지를 과거에서 현재 순서로 반환합니다.")
    MessagePageResponse getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int size
    ) {
        return messageService.getMessages(conversationId, currentUserId(jwt), before, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "메시지 전송", description = "텍스트·첨부파일을 일반 또는 예약 전송합니다.")
    MessageResponse send(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.send(conversationId, currentUserId(jwt), request);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

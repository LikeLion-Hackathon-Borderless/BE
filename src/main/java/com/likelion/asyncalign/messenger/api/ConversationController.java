package com.likelion.asyncalign.messenger.api;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.messenger.application.ConversationService;
import com.likelion.asyncalign.messenger.dto.ConversationResponse;
import com.likelion.asyncalign.messenger.dto.CreateDirectConversationRequest;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "대화", description = "1:1 대화방 생성, 목록, 읽음 처리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "1:1 대화방 생성", description = "두 사용자 사이에 기존 대화방이 있으면 해당 대화방을 반환합니다.")
    ConversationResponse createDirect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDirectConversationRequest request
    ) {
        return conversationService.createDirect(currentUserId(jwt), request.otherUserId());
    }

    @GetMapping
    @Operation(summary = "대화방 목록 조회", description = "최근 활동 시각 내림차순으로 내 1:1 대화방을 반환합니다.")
    List<ConversationResponse> getConversations(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.getConversations(currentUserId(jwt));
    }

    @PutMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "대화방 읽음 처리", description = "요청 시점까지의 메시지를 읽음 처리합니다.")
    void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
        conversationService.markRead(conversationId, currentUserId(jwt));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

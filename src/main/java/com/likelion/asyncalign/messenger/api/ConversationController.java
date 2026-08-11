package com.likelion.asyncalign.messenger.api;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.messenger.application.ConversationService;
import com.likelion.asyncalign.messenger.dto.ConversationResponse;
import com.likelion.asyncalign.messenger.dto.CreateDirectConversationRequest;
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
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse createDirect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDirectConversationRequest request
    ) {
        return conversationService.createDirect(currentUserId(jwt), request.otherUserId());
    }

    @GetMapping
    List<ConversationResponse> getConversations(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.getConversations(currentUserId(jwt));
    }

    @PutMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
        conversationService.markRead(conversationId, currentUserId(jwt));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

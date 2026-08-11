package com.likelion.asyncalign.messenger.api;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.messenger.application.MessageService;
import com.likelion.asyncalign.messenger.dto.MessagePageResponse;
import com.likelion.asyncalign.messenger.dto.MessageResponse;
import com.likelion.asyncalign.messenger.dto.SendMessageRequest;
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
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
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
    MessageResponse send(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return messageService.send(conversationId, currentUserId(jwt), request.content());
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

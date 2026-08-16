package com.likelion.asyncalign.messenger.dto;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.user.dto.UserSummaryResponse;

public record ConversationResponse(
        UUID id,
        String type,
        UserSummaryResponse otherParticipant,
        LatestMessage latestMessage,
        long unreadCount,
        Instant lastActivityAt
) {
    public record LatestMessage(
            UUID id,
            UUID senderId,
            String content,
            Instant sentAt
    ) {
    }
}

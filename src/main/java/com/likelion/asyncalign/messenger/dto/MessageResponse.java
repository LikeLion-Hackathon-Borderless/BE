package com.likelion.asyncalign.messenger.dto;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.user.domain.User;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        Sender sender,
        String content,
        Instant sentAt,
        ZonedDateTime senderLocalSentAt,
        ZonedDateTime viewerLocalSentAt
) {
    public static MessageResponse from(Message message, User viewer) {
        User sender = message.getSender();
        Instant sentAt = message.getCreatedAt();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                new Sender(sender.getId(), sender.getDisplayName(), sender.getTimeZoneId()),
                message.getContent(),
                sentAt,
                sentAt.atZone(java.time.ZoneId.of(sender.getTimeZoneId())),
                sentAt.atZone(java.time.ZoneId.of(viewer.getTimeZoneId())));
    }

    public record Sender(UUID id, String displayName, String timeZoneId) {
    }
}

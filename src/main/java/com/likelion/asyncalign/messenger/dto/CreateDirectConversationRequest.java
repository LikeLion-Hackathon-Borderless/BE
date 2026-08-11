package com.likelion.asyncalign.messenger.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateDirectConversationRequest(@NotNull UUID otherUserId) {
}

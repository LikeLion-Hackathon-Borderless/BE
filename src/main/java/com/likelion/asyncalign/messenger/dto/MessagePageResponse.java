package com.likelion.asyncalign.messenger.dto;

import java.time.Instant;
import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        boolean hasMore,
        Instant nextBefore
) {
}

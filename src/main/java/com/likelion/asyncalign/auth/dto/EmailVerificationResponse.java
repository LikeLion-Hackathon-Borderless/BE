package com.likelion.asyncalign.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationResponse(UUID verificationToken, Instant verifiedAt) {
}

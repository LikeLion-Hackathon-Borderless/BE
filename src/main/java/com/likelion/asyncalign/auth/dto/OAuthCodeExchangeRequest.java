package com.likelion.asyncalign.auth.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record OAuthCodeExchangeRequest(@NotNull UUID code) {
}

package com.likelion.asyncalign.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateEmailInvitationsRequest(
        @NotEmpty @Size(max = 20) List<@Email @Size(max = 320) String> emails
) {
}

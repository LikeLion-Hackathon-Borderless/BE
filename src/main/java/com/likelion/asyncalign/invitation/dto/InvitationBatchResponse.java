package com.likelion.asyncalign.invitation.dto;

import java.util.List;

public record InvitationBatchResponse(List<Result> results) {
    public record Result(String email, String status, String errorCode) {
    }
}

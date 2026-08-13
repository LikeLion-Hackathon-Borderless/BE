package com.likelion.asyncalign.user.dto;

import com.likelion.asyncalign.user.domain.WorkRole;

public record WorkRoleResponse(String code, String label, boolean customInputRequired) {

    public static WorkRoleResponse from(WorkRole role) {
        return new WorkRoleResponse(role.name(), role.label(), role == WorkRole.OTHER);
    }
}

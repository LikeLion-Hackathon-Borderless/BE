package com.likelion.asyncalign.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @Schema(example = "Global Async Team")
        @NotBlank @Size(max = 80) String name,
        @Schema(description = "URL이 아닌 조직 도메인", example = "company.com", nullable = true)
        @Size(max = 253)
        @Pattern(
                regexp = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$",
                message = "URL이 아닌 유효한 도메인을 입력해 주세요.")
        String organizationDomain
) {
}

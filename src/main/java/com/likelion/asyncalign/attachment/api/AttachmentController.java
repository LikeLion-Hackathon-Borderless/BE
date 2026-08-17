package com.likelion.asyncalign.attachment.api;

import com.likelion.asyncalign.attachment.application.AttachmentService;
import com.likelion.asyncalign.attachment.dto.AttachmentResponse;
import com.likelion.asyncalign.global.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "첨부파일", description = "대화 첨부파일 업로드, 상태 조회, 다운로드")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/conversations/{conversationId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "첨부파일 업로드")
    AttachmentResponse upload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID conversationId,
            @RequestPart("file") MultipartFile file
    ) {
        return attachmentService.upload(conversationId, userId(jwt), file);
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "첨부파일 상태 조회")
    AttachmentResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attachmentId
    ) {
        return attachmentService.get(attachmentId, userId(jwt));
    }

    @GetMapping("/attachments/{attachmentId}/content")
    @Operation(summary = "첨부파일 다운로드")
    ResponseEntity<FileSystemResource> download(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attachmentId
    ) {
        AttachmentService.DownloadFile file = attachmentService.download(attachmentId, userId(jwt));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

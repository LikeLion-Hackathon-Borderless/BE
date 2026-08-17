package com.likelion.asyncalign.attachment.application;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentFileValidator {

    public static final long MAX_SIZE = 10L * 1024 * 1024;

    private static final Map<String, String> MIME_EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "text/plain", ".txt",
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp");

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "업로드할 파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ApiException(ErrorCode.FILE_SIZE_EXCEEDED, "첨부파일은 최대 10MB까지 업로드할 수 있습니다.");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = MIME_EXTENSIONS.get(contentType);
        if (extension == null || !hasMatchingSignature(file, contentType)) {
            throw new ApiException(
                    ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "PDF, DOCX, TXT, PNG, JPG, WEBP 파일만 업로드할 수 있습니다.");
        }
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(extension)
                && !(contentType.equals("image/jpeg") && lowerName.endsWith(".jpeg"))) {
            throw new ApiException(ErrorCode.UNSUPPORTED_FILE_TYPE, "파일 확장자와 MIME 형식이 일치하지 않습니다.");
        }
        return new ValidatedFile(originalName, contentType, extension);
    }

    private boolean hasMatchingSignature(MultipartFile file, String contentType) {
        try {
            byte[] bytes = file.getBytes();
            return switch (contentType) {
                case "application/pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
                case "image/png" -> startsWith(bytes, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
                case "image/jpeg" -> bytes.length >= 3
                        && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
                case "image/webp" -> bytes.length >= 12
                        && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> isDocx(bytes);
                case "text/plain" -> isUtf8Text(bytes);
                default -> false;
            };
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED, "첨부파일을 읽을 수 없습니다.");
        }
    }

    private boolean isDocx(byte[] bytes) throws IOException {
        if (!startsWith(bytes, new byte[]{'P', 'K'})) {
            return false;
        }
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            for (java.util.zip.ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if ("word/document.xml".equals(entry.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUtf8Text(byte[] bytes) {
        if (bytes.length == 0) {
            return false;
        }
        for (byte value : bytes) {
            if (value == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (java.nio.charset.CharacterCodingException exception) {
            return false;
        }
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeFileName(String originalName) {
        String name = originalName == null ? "file" : java.nio.file.Path.of(originalName).getFileName().toString();
        name = name.replaceAll("[\\r\\n\\t]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    public record ValidatedFile(String originalName, String contentType, String extension) {
    }
}

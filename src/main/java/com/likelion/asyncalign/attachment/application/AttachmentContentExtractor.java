package com.likelion.asyncalign.attachment.application;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentContentExtractor {

    private static final int MAX_EXTRACTED_CHARACTERS = 20_000;

    public String extract(MultipartFile file, String contentType) throws Exception {
        String text = switch (contentType) {
            case "text/plain" -> new String(file.getBytes(), StandardCharsets.UTF_8);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractDocx(file.getBytes());
            default -> null;
        };
        if (text == null) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > MAX_EXTRACTED_CHARACTERS
                ? normalized.substring(0, MAX_EXTRACTED_CHARACTERS)
                : normalized;
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (java.util.zip.ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return xml
                            .replaceAll("</w:p>", "\n")
                            .replaceAll("<[^>]+>", " ")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&");
                }
            }
        }
        throw new IllegalArgumentException("DOCX document body is missing");
    }
}

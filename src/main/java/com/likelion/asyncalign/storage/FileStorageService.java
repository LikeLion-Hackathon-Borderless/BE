package com.likelion.asyncalign.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path root;
    private final String publicBaseUrl;
    private final String publicPath;

    public FileStorageService(
            @Value("${app.storage.root}") String root,
            @Value("${app.public-base-url}") String publicBaseUrl,
            @Value("${app.storage.public-path:/uploads}") String publicPath
    ) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        this.publicPath = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
    }

    public String storeProfileImage(UUID userId, MultipartFile file) {
        if (file.isEmpty() || !EXTENSIONS.containsKey(file.getContentType())) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED, "JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
        String filename = userId + "-" + UUID.randomUUID() + EXTENSIONS.get(file.getContentType());
        Path profileDirectory = root.resolve("profiles").normalize();
        Path target = profileDirectory.resolve(filename).normalize();
        if (!target.startsWith(profileDirectory)) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED, "올바르지 않은 파일 경로입니다.");
        }
        try {
            Files.createDirectories(profileDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicBaseUrl + publicPath + "/profiles/" + filename;
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED, "프로필 이미지 저장에 실패했습니다.");
        }
    }

    public Path getRoot() {
        return root;
    }

    public String getPublicPath() {
        return publicPath;
    }
}

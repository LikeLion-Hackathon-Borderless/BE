package com.likelion.asyncalign.user.api;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.dto.UserResponse;
import com.likelion.asyncalign.user.dto.UpdateProfileRequest;
import com.likelion.asyncalign.user.dto.UpdateWorkContextRequest;
import com.likelion.asyncalign.user.dto.WorkRoleResponse;
import com.likelion.asyncalign.user.domain.WorkRole;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMe(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/me/profile")
    UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }

    @PatchMapping("/me/work-context")
    UserResponse updateWorkContext(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateWorkContextRequest request
    ) {
        return userService.updateWorkContext(UUID.fromString(jwt.getSubject()), request);
    }

    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UserResponse uploadProfileImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {
        return userService.uploadProfileImage(UUID.fromString(jwt.getSubject()), file);
    }

    @GetMapping("/roles")
    List<WorkRoleResponse> getRoles() {
        return java.util.Arrays.stream(WorkRole.values())
                .map(WorkRoleResponse::from)
                .toList();
    }

    @GetMapping
    List<UserResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(UUID.fromString(jwt.getSubject()), query, size);
    }
}

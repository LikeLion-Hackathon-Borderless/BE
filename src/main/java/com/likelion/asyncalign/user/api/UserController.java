package com.likelion.asyncalign.user.api;

import java.util.List;
import java.util.UUID;

import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    List<UserResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(UUID.fromString(jwt.getSubject()), query, size);
    }
}

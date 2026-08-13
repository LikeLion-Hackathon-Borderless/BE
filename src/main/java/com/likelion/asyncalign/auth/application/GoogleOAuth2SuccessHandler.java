package com.likelion.asyncalign.auth.application;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuthLoginCodeService loginCodeService;
    private final String successRedirect;

    public GoogleOAuth2SuccessHandler(
            AuthService authService,
            OAuthLoginCodeService loginCodeService,
            @Value("${app.oauth2.success-redirect:http://localhost:3000/oauth/callback}") String successRedirect
    ) {
        this.authService = authService;
        this.loginCodeService = loginCodeService;
        this.successRedirect = successRedirect;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google 계정 이메일을 확인할 수 없습니다.");
            return;
        }

        var user = authService.findOrCreateGoogleUser(email, name, picture);
        var loginCode = loginCodeService.create(user);
        String redirectUrl = UriComponentsBuilder.fromUriString(successRedirect)
                .queryParam("code", loginCode)
                .build(true)
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}

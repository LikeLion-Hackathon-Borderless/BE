package com.likelion.asyncalign.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.auth.dto.LoginRequest;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JavaMailSender mailSender;

    @Test
    void signUpLoginAndGetMe() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "verify@example.com"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "verify@example.com",
                                "code", "419203"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationToken").exists());

        SignUpRequest signUp = new SignUpRequest(
                "seoyeon@example.com",
                "password123!",
                "이서연",
                null,
                true);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("seoyeon@example.com"));

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("seoyeon@example.com", "password123!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readValue(loginBody, Map.class).get("accessToken").toString();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("이서연"));

        mockMvc.perform(get("/api/v1/users/roles")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DEVELOPER"))
                .andExpect(jsonPath("$[11].customInputRequired").value(true));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "이서연",
                                "role", "PROJECT_MANAGER",
                                "preferredLanguage", "ko"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROJECT_MANAGER"));

        mockMvc.perform(patch("/api/v1/users/me/work-context")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "timeZoneId", "Asia/Seoul",
                                "workStart", "09:00:00",
                                "workEnd", "18:00:00",
                                "workDays", new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"}))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZoneId").value("Asia/Seoul"))
                .andExpect(jsonPath("$.onboardingStep").value("WORKSPACE"));

        MockMultipartFile profileImage = new MockMultipartFile(
                "file", "profile.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(profileImage)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(org.hamcrest.Matchers.containsString("/uploads/profiles/")));

    }
}

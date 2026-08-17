package com.likelion.asyncalign.mvp;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvitationMessagingAttachmentIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JavaMailSender mailSender;

    @Test
    void invitationWorkspaceScopedMessagingAndAttachmentFlow() throws Exception {
        Account owner = signUp("invite-owner");
        Account member = signUp("invite-member");
        Account outsider = signUp("invite-outsider");
        UUID workspaceId = createWorkspace(owner, "Global Team");

        Map<String, Object> firstLink = createLink(owner, workspaceId, false);
        String firstToken = firstLink.get("token").toString();
        mockMvc.perform(get("/api/v1/workspace-invitations/{token}", firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceName").value("Global Team"));

        mockMvc.perform(post("/api/v1/workspace-invitations/{token}/accept", firstToken)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.myMembershipRole").value("MEMBER"));

        assertEquals(firstToken, createLink(owner, workspaceId, false).get("token"));
        String regenerated = createLink(owner, workspaceId, true).get("token").toString();
        assertNotEquals(firstToken, regenerated);
        mockMvc.perform(get("/api/v1/workspace-invitations/{token}", firstToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));

        String pendingEmail = "pending-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/invitations", workspaceId)
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "emails", List.of(member.email(), pendingEmail, pendingEmail.toUpperCase())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].status").value("ALREADY_MEMBER"))
                .andExpect(jsonPath("$.results[1].status").value("SENT"));
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        Matcher tokenMatcher = Pattern.compile("/invitations/(wsi_[^\\s]+)")
                .matcher(mailCaptor.getValue().getText());
        tokenMatcher.find();
        String emailInviteToken = tokenMatcher.group(1);
        mockMvc.perform(post("/api/v1/workspace-invitations/{token}/accept", emailInviteToken)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITATION_EMAIL_MISMATCH"));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearer(owner.token()))
                        .param("workspaceId", workspaceId.toString())
                        .param("query", "invite-member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(member.userId().toString()));
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearer(outsider.token()))
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ACCESS_DENIED"));

        String conversationBody = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workspaceId", workspaceId,
                                "otherUserId", member.userId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID conversationId = UUID.fromString(readMap(conversationBody).get("id").toString());

        byte[] fileBytes = "문서 3번 섹션의 완료 기준을 확인합니다.".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "spec.txt", "text/plain", fileBytes);
        String attachmentBody = mockMvc.perform(multipart(
                                "/api/v1/conversations/{conversationId}/attachments", conversationId)
                        .file(file)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processingStatus").value("READY"))
                .andReturn().getResponse().getContentAsString();
        UUID attachmentId = UUID.fromString(readMap(attachmentBody).get("id").toString());

        mockMvc.perform(get("/api/v1/attachments/{attachmentId}/content", attachmentId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(result -> assertArrayEquals(fileBytes, result.getResponse().getContentAsByteArray()));

        mockMvc.perform(get("/api/v1/attachments/{attachmentId}/content", attachmentId)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));

        String messageBody = mockMvc.perform(post(
                                "/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "스펙 검토 부탁드립니다.",
                                "attachmentIds", List.of(attachmentId),
                                "deliveryMode", "AS_IS"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmationStatus").value("UNCONFIRMED"))
                .andExpect(jsonPath("$.attachments[0].id").value(attachmentId.toString()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].attachments[0].originalFileName").value("spec.txt"));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "예약 메시지",
                                "attachmentIds", List.of(),
                                "deliveryMode", "AS_IS",
                                "scheduledFor", Instant.now().plusSeconds(3600).toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryStatus").value("SCHEDULED"));

        MockMultipartFile invalid = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/conversations/{conversationId}/attachments", conversationId)
                        .file(invalid)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    private Account signUp(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@example.com";
        SignUpRequest request = new SignUpRequest(email, "password123!", name, null, true);
        String body = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> response = readMap(body);
        UUID userId = UUID.fromString(((Map<?, ?>) response.get("user")).get("id").toString());
        return new Account(userId, email, response.get("accessToken").toString());
    }

    private UUID createWorkspace(Account owner, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "organizationDomain", "example.com"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(readMap(body).get("id").toString());
    }

    private Map<String, Object> createLink(Account owner, UUID workspaceId, boolean regenerate) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/invitation-links", workspaceId)
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "expiresInDays", 7,
                                "regenerate", regenerate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readMap(body);
    }

    private Map<String, Object> readMap(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() {});
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(UUID userId, String email, String token) {
    }
}

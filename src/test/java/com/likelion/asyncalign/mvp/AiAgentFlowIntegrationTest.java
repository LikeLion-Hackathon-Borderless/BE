package com.likelion.asyncalign.mvp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.alignment.application.AiAgentClient;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAgentFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JavaMailSender mailSender;

    @MockitoBean
    AiAgentClient aiAgentClient;

    @Test
    void repeatsAgentInterruptsUntilTheCardIsDone() throws Exception {
        Account sender = signUp("agent-sender");
        Account recipient = signUp("agent-recipient");
        UUID workspaceId = createWorkspace(sender);
        acceptInvite(sender, recipient, workspaceId);
        UUID conversationId = createConversation(sender, recipient, workspaceId);

        AiAgentClient.AmbiguityItem item = new AiAgentClient.AmbiguityItem(
                "내일까지",
                "TIME",
                "정확한 시각이 없습니다.",
                List.of("2026-08-20T18:00:00+09:00", "custom"),
                "정확한 기한을 선택해 주세요.");
        when(aiAgentClient.isEnabled()).thenReturn(true);
        when(aiAgentClient.start(any())).thenReturn(new AiAgentClient.SessionResult(
                "thread-1",
                "interrupt",
                new AiAgentClient.InterruptPayload(1, 1, item),
                null));

        String createBody = mockMvc.perform(post(
                                "/api/v1/conversations/{conversationId}/ai-reviews", conversationId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"이 문서 내일까지 검토 부탁드립니다.\",\"attachmentIds\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("DITTO_AGENT"))
                .andExpect(jsonPath("$.agentSession.status").value("INTERRUPT"))
                .andExpect(jsonPath("$.agentSession.step").value(1))
                .andExpect(jsonPath("$.agentSession.item.category").value("TIME"))
                .andReturn().getResponse().getContentAsString();
        UUID reviewId = UUID.fromString(readMap(createBody).get("id").toString());

        mockMvc.perform(post("/api/v1/ai-reviews/{reviewId}/answers", reviewId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"2026-08-20T18:00:00+09:00\"}"))
                .andExpect(status().isForbidden());

        AiAgentClient.ConfirmedCard card = new AiAgentClient.ConfirmedCard(
                "문서 검토",
                recipient.name(),
                "2026-08-20T18:00:00+09:00",
                "2026-08-20T02:00:00-07:00",
                "검토 요청",
                "제안(결정 아님)",
                "검토 결과와 필수 수정사항 목록",
                null,
                List.of(),
                new AiAgentClient.Conflict(
                        "2026-08-20T02:00:00-07:00",
                        false,
                        "수신자 근무시간 밖"),
                "이 문서 내일까지 검토 부탁드립니다.");
        when(aiAgentClient.answer(eq("thread-1"), any())).thenReturn(new AiAgentClient.SessionResult(
                "thread-1", "done", null, card));

        mockMvc.perform(post("/api/v1/ai-reviews/{reviewId}/answers", reviewId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"2026-08-20T18:00:00+09:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentSession.status").value("DONE"))
                .andExpect(jsonPath("$.structuredFields.task.value").value("문서 검토"))
                .andExpect(jsonPath("$.structuredFields.deadline.value").value("2026-08-20T09:00:00Z"))
                .andExpect(jsonPath("$.structuredFields.expectedOutcome.value")
                        .value("검토 결과와 필수 수정사항 목록"));

        mockMvc.perform(post("/api/v1/ai-reviews/{reviewId}/answers", reviewId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"다시\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI_AGENT_INVALID_STATE"));
    }

    @Test
    void fallsBackSafelyAndReturnsAWarningWhenTheAgentIsUnavailable() throws Exception {
        reset(aiAgentClient);
        Account sender = signUp("fallback-sender");
        Account recipient = signUp("fallback-recipient");
        UUID workspaceId = createWorkspace(sender);
        acceptInvite(sender, recipient, workspaceId);
        UUID conversationId = createConversation(sender, recipient, workspaceId);

        when(aiAgentClient.isEnabled()).thenReturn(true);
        when(aiAgentClient.start(any())).thenThrow(
                new AiAgentClient.AiAgentClientException(502, "AI service is unavailable"));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/ai-reviews", conversationId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"이 문서 내일까지 검토 부탁드립니다.\",\"attachmentIds\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("DITTO_AI_FAILURE"))
                .andExpect(jsonPath("$.agentSession").doesNotExist())
                .andExpect(jsonPath("$.warnings[?(@.code == 'AI_REVIEW_FAILED')]").exists());
    }

    private Account signUp(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignUpRequest(email, "password123!", name, null, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> response = readMap(body);
        return new Account(
                UUID.fromString(((Map<?, ?>) response.get("user")).get("id").toString()),
                name,
                response.get("accessToken").toString());
    }

    private UUID createWorkspace(Account sender) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI Agent Team\",\"organizationDomain\":null}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(readMap(body).get("id").toString());
    }

    private void acceptInvite(Account sender, Account recipient, UUID workspaceId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/invitation-links", workspaceId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresInDays\":7,\"regenerate\":false}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = readMap(body).get("token").toString();
        mockMvc.perform(post("/api/v1/workspace-invitations/{token}/accept", token)
                        .header("Authorization", bearer(recipient.token())))
                .andExpect(status().isOk());
    }

    private UUID createConversation(Account sender, Account recipient, UUID workspaceId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workspaceId", workspaceId,
                                "otherUserId", recipient.userId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(readMap(body).get("id").toString());
    }

    private Map<String, Object> readMap(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() {});
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(UUID userId, String name, String token) {
    }
}

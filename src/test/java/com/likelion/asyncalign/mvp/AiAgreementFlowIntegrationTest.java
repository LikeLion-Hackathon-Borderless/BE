package com.likelion.asyncalign.mvp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAgreementFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JavaMailSender mailSender;

    @Test
    void aiReviewCardRevisionAgreementAndUnderstandThisFlow() throws Exception {
        Account sender = signUp("ai-sender");
        Account recipient = signUp("ai-recipient");
        UUID workspaceId = createWorkspace(sender);
        String inviteToken = createInviteLink(sender, workspaceId);
        mockMvc.perform(post("/api/v1/workspace-invitations/{token}/accept", inviteToken)
                        .header("Authorization", bearer(recipient.token())))
                .andExpect(status().isOk());
        UUID conversationId = createConversation(sender, recipient, workspaceId);

        UUID attachmentId = uploadText(sender, conversationId);
        String reviewBody = mockMvc.perform(post(
                                "/api/v1/conversations/{conversationId}/ai-reviews", conversationId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "이 부분 내일까지 검토 부탁드려요. 이 방향도 좋은데 조금 더 고민해 주세요.",
                                "attachmentIds", List.of(attachmentId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.provider").value("LOCAL_FALLBACK"))
                .andExpect(jsonPath("$.structuredFields.deadline.value").doesNotExist())
                .andExpect(jsonPath("$.warnings[0].code").value("AMBIGUOUS_DEADLINE"))
                .andExpect(jsonPath("$.evidence[0].attachmentId").value(attachmentId.toString()))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> reviewJson = readMap(reviewBody);
        UUID reviewId = UUID.fromString(reviewJson.get("id").toString());
        UUID evidenceId = UUID.fromString(
                ((Map<?, ?>) ((List<?>) reviewJson.get("evidence")).getFirst()).get("id").toString());

        mockMvc.perform(patch("/api/v1/ai-reviews/{reviewId}", reviewId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(patch("/api/v1/ai-reviews/{reviewId}", reviewId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "task", "문서 3번 섹션 검토",
                                "confirmed", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        Instant deadline = Instant.now().plus(Duration.ofDays(2));
        mockMvc.perform(patch("/api/v1/ai-reviews/{reviewId}", reviewId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "task", "문서 3번 섹션 검토",
                                "assigneeUserId", recipient.userId(),
                                "deadline", deadline.toString(),
                                "expectedOutcome", "방향 유지와 필수 수정사항 목록",
                                "confirmedEvidenceIds", List.of(evidenceId),
                                "confirmed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.structuredFields.task.confirmed").value(true))
                .andExpect(jsonPath("$.evidence[0].confirmed").value(true));

        String sentBody = mockMvc.perform(post("/api/v1/ai-reviews/{reviewId}/send", reviewId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "문서 3번 섹션 검토 부탁드립니다."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryMode").value("AI_REVIEW_CONFIRMED"))
                .andExpect(jsonPath("$.confirmationStatus").value("REVIEW"))
                .andExpect(jsonPath("$.understandingCard.state").value("REVIEW"))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> sentJson = readMap(sentBody);
        UUID cardId = UUID.fromString(((Map<?, ?>) sentJson.get("understandingCard")).get("id").toString());

        mockMvc.perform(post("/api/v1/understanding-cards/{cardId}/responses", cardId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AGREE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CARD_RESPONSE_NOT_ALLOWED"));

        mockMvc.perform(get("/api/v1/understanding-cards/{cardId}", cardId)
                        .header("Authorization", bearer(recipient.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task").value("문서 3번 섹션 검토"))
                .andExpect(jsonPath("$.deadline.viewerTimeZoneId").value("UTC"))
                .andExpect(jsonPath("$.attachments[0].id").value(attachmentId.toString()));

        Instant proposed = deadline.plus(Duration.ofDays(1));
        mockMvc.perform(post("/api/v1/understanding-cards/{cardId}/responses", cardId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "REQUEST_DEADLINE_CHANGE",
                                "comment", "하루 더 필요합니다.",
                                "proposedDeadline", proposed.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.latestResponse.type").value("REQUEST_DEADLINE_CHANGE"));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}/agreement-logs", conversationId)
                        .header("Authorization", bearer(sender.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs[0].status").value("PENDING"));

        mockMvc.perform(post("/api/v1/understanding-cards/{cardId}/revisions", cardId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "task", "문서 3번 섹션 검토",
                                "deadline", proposed.toString(),
                                "expectedOutcome", "필수 수정사항 목록과 승인 여부",
                                "changeNote", "요청한 기한과 완료 기준을 반영했습니다."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.revision").value(2));

        mockMvc.perform(post("/api/v1/understanding-cards/{cardId}/responses", cardId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AGREE\",\"comment\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AGREED"));

        mockMvc.perform(post("/api/v1/understanding-cards/{cardId}/responses", cardId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AGREE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARD_INVALID_STATE"));

        mockMvc.perform(get("/api/v1/conversations/{conversationId}/agreement-logs", conversationId)
                        .header("Authorization", bearer(sender.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs.length()").value(2))
                .andExpect(jsonPath("$.logs[1].status").value("AGREED"))
                .andExpect(jsonPath("$.logs[1].revision").value(2))
                .andExpect(jsonPath("$.logs[1].fileReferences[0].attachmentId").value(attachmentId.toString()));

        String normalMessageBody = mockMvc.perform(post(
                                "/api/v1/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "배포 스크립트 점검 부탁해요.",
                                "attachmentIds", List.of(),
                                "deliveryMode", "AS_IS"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID normalMessageId = UUID.fromString(readMap(normalMessageBody).get("id").toString());

        String understandBody = mockMvc.perform(post(
                                "/api/v1/messages/{messageId}/understanding-cards", normalMessageId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"includeConversationContext\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.needsClarification").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID understandCardId = UUID.fromString(readMap(understandBody).get("id").toString());

        mockMvc.perform(post("/api/v1/messages/{messageId}/understanding-cards", normalMessageId)
                        .header("Authorization", bearer(recipient.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"includeConversationContext\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(understandCardId.toString()));
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
                response.get("accessToken").toString());
    }

    private UUID createWorkspace(Account sender) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"AI Agreement Team\",\"organizationDomain\":null}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(readMap(body).get("id").toString());
    }

    private String createInviteLink(Account sender, UUID workspaceId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/invitation-links", workspaceId)
                        .header("Authorization", bearer(sender.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresInDays\":7,\"regenerate\":false}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readMap(body).get("token").toString();
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

    private UUID uploadText(Account sender, UUID conversationId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "requirements.txt",
                "text/plain",
                "문서 3번 섹션 검토와 완료 기준".getBytes(StandardCharsets.UTF_8));
        String body = mockMvc.perform(multipart(
                                "/api/v1/conversations/{conversationId}/attachments", conversationId)
                        .file(file)
                        .header("Authorization", bearer(sender.token())))
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

    private record Account(UUID userId, String token) {
    }
}

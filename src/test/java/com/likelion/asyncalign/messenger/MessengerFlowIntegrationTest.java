package com.likelion.asyncalign.messenger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessengerFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createDirectConversationAndSendMessage() throws Exception {
        Map<String, Object> seoyeon = signUp("sender@example.com", "이서연", "Asia/Seoul");
        Map<String, Object> alex = signUp("receiver@example.com", "Alex", "America/Los_Angeles");
        String senderToken = seoyeon.get("accessToken").toString();
        UUID alexId = UUID.fromString(((Map<?, ?>) alex.get("user")).get("id").toString());

        String conversationBody = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("otherUserId", alexId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otherParticipant.displayName").value("Alex"))
                .andReturn().getResponse().getContentAsString();
        UUID conversationId = UUID.fromString(
                objectMapper.readValue(conversationBody, new TypeReference<Map<String, Object>>() {}).get("id").toString());

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "내일까지 검토 부탁드려요."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderLocalSentAt").exists())
                .andExpect(jsonPath("$.viewerLocalSentAt").exists());

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content").value("내일까지 검토 부탁드려요."));
    }

    private Map<String, Object> signUp(String email, String name, String timeZone) throws Exception {
        SignUpRequest request = new SignUpRequest(
                email,
                "password123!",
                name,
                timeZone,
                "en",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0));
        String body = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, new TypeReference<>() {});
    }
}

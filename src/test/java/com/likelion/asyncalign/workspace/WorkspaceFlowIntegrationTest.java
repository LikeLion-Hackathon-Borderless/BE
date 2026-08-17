package com.likelion.asyncalign.workspace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import com.likelion.asyncalign.workspace.domain.Workspace;
import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import com.likelion.asyncalign.workspace.domain.WorkspaceMemberRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRepository;
import com.likelion.asyncalign.workspace.domain.WorkspaceRole;
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
class WorkspaceFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository memberRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void workspaceLifecyclePermissionsAndWorkContextOverride() throws Exception {
        Account owner = signUp("owner");
        Account member = signUp("member");
        Account outsider = signUp("outsider");

        String createBody = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Global Async Team",
                                "organizationDomain", "Company.COM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Global Async Team"))
                .andExpect(jsonPath("$.organizationDomain").value("company.com"))
                .andExpect(jsonPath("$.myMembershipRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        UUID workspaceId = UUID.fromString(readMap(createBody).get("id").toString());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingStep").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workspaceId.toString()));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ACCESS_DENIED"));

        addMember(workspaceId, member.userId());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].user.email").exists())
                .andExpect(jsonPath("$[0].workContext.overridden").value(false));

        mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/members/me/work-context", workspaceId)
                        .header("Authorization", bearer(member.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeZoneId": "America/Los_Angeles",
                                  "workStart": "08:30:00",
                                  "workEnd": "17:30:00",
                                  "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overridden").value(true))
                .andExpect(jsonPath("$.timeZoneId").value("America/Los_Angeles"))
                .andExpect(jsonPath("$.workStart").value("08:30:00"))
                .andExpect(jsonPath("$.workEnd").value("17:30:00"))
                .andExpect(jsonPath("$.workDays.length()").value(4));

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_OWNER_REQUIRED"));

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/members/me/work-context", workspaceId)
                        .header("Authorization", bearer(member.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKSPACE_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ALREADY_DELETED"));
    }

    @Test
    void rejectsInvalidWorkspaceAndWorkContextInputs() throws Exception {
        Account owner = signUp("validation-owner");

        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","organizationDomain":"https://company.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        String body = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Validation Team","organizationDomain":null}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID workspaceId = UUID.fromString(readMap(body).get("id").toString());

        mockMvc.perform(put("/api/v1/workspaces/{workspaceId}/members/me/work-context", workspaceId)
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeZoneId":"Invalid/Zone",
                                  "workStart":"18:00:00",
                                  "workEnd":"09:00:00",
                                  "workDays":["MONDAY"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Account signUp(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        SignUpRequest request = new SignUpRequest(email, "password123!", prefix, null, true);
        String body = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> response = readMap(body);
        UUID userId = UUID.fromString(((Map<?, ?>) response.get("user")).get("id").toString());
        return new Account(userId, response.get("accessToken").toString());
    }

    private void addMember(UUID workspaceId, UUID userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        memberRepository.saveAndFlush(new WorkspaceMember(workspace, user, WorkspaceRole.MEMBER));
    }

    private Map<String, Object> readMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(UUID userId, String token) {
    }
}

package com.errorpurifier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersDeviceAndPreparesAnAnalysisPrompt() throws Exception {
        String syncBody = mockMvc.perform(post("/api/v1/client/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"\",\"pluginVersion\":\"1.0.0-test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceUuid").isNotEmpty())
                .andExpect(jsonPath("$.parsingRules").isArray())
                .andReturn().getResponse().getContentAsString();
        JsonNode syncResponse = objectMapper.readTree(syncBody);
        String deviceId = syncResponse.get("deviceUuid").asText();

        mockMvc.perform(post("/api/v1/prompt/prepare")
                        .header("X-Device-UUID", deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawLog":"java.lang.IllegalStateException: sample failure\\n  at com.example.App.run(App.java:42)",
                                  "selectedText":null,
                                  "projectFiles":{"build.gradle":"plugins { id 'java' }"},
                                  "environmentTags":{"ide":"intellij"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisReady").value(true))
                .andExpect(jsonPath("$.cacheHit").value(false))
                .andExpect(jsonPath("$.preparedPrompt").value(org.hamcrest.Matchers.containsString("IllegalStateException")));
    }

    @Test
    void rejectsAnUnknownDeviceDuringSynchronization() throws Exception {
        mockMvc.perform(post("/api/v1/client/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"00000000-0000-0000-0000-000000000001\",\"pluginVersion\":\"1.0.0-test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsNotFoundForAnUnknownEndpoint() throws Exception {
        String response = mockMvc.perform(get("/unknown-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("요청한 경로를 찾을 수 없습니다.");
    }

    @Test
    void exposesSeededDiagnosticPlaybooksOnlyToAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/admin/diagnostic-playbooks")
                .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'LOMBOK_ANNOTATION_PROCESSING')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name == 'HIKARI_CONNECTION_POOL')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name == 'JPA_LAZY_LOADING')]").isNotEmpty());
    }

    @Test
    void previewsActiveDiagnosticPlaybookMatchesForAdministrators() throws Exception {
        mockMvc.perform(post("/api/v1/admin/diagnostic-playbooks/preview")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"log\":\"java.sql.SQLIntegrityConstraintViolationException: Column 'email' cannot be null\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'DATABASE_CONSTRAINT')]").isNotEmpty());

        mockMvc.perform(post("/api/v1/admin/diagnostic-playbooks/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"log\":\"sample\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void previewsAPlaybookPatternBeforeItIsSaved() throws Exception {
        mockMvc.perform(post("/api/v1/admin/diagnostic-playbooks/preview-pattern")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchPattern\":\"Cannot find symbol\",\"log\":\"error: cannot find symbol\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true));

        mockMvc.perform(post("/api/v1/admin/diagnostic-playbooks/preview-pattern")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchPattern\":\"[invalid\",\"log\":\"sample\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesMatchingDiagnosticPlaybooksInPreparedPromptResponse() throws Exception {
        String syncBody = mockMvc.perform(post("/api/v1/client/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"\",\"pluginVersion\":\"1.0.0-test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String deviceId = objectMapper.readTree(syncBody).get("deviceUuid").asText();

        mockMvc.perform(post("/api/v1/prompt/prepare")
                        .header("X-Device-UUID", deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rawLog":"org.springframework.dao.DataIntegrityViolationException: Column 'email' cannot be null\\njava.sql.SQLIntegrityConstraintViolationException: Column 'email' cannot be null",
                                  "projectFiles":{"build.gradle":"plugins { id 'java' }"},
                                  "environmentTags":{"ide":"intellij"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosticPlaybooks").value(org.hamcrest.Matchers.hasItem("DATABASE_CONSTRAINT")))
                .andExpect(jsonPath("$.preparedPrompt").value(org.hamcrest.Matchers.containsString("DB 제약 조건 위반")))
                .andExpect(jsonPath("$.preparedPrompt").value(org.hamcrest.Matchers.containsString("Suppressed:")));

        String playbooks = mockMvc.perform(get("/api/v1/admin/diagnostic-playbooks")
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode playbook : objectMapper.readTree(playbooks)) {
            if ("DATABASE_CONSTRAINT".equals(playbook.get("name").asText())) {
                assertThat(playbook.get("matchCount").asLong()).isGreaterThanOrEqualTo(1);
                return;
            }
        }
        throw new AssertionError("DATABASE_CONSTRAINT 플레이북을 찾을 수 없습니다.");
    }

    @Test
    void managesDiagnosticPlaybooksWithAnAdminToken() throws Exception {
        String request = """
                {
                  "name":"TEST_ADMIN_PLAYBOOK",
                  "matchPattern":"TestAdminException",
                  "guidance":"관리 화면 CRUD 검증용 가이드입니다.",
                  "priority":321
                }
                """;

        String created = mockMvc.perform(post("/api/v1/admin/diagnostic-playbooks")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/diagnostic-playbooks/{id}", id)
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("321", "654")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(654));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/admin/diagnostic-playbooks/{id}/active", id)
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/admin/index.html"));

        mockMvc.perform(get("/admin/index.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Error Purifier")));
    }
}

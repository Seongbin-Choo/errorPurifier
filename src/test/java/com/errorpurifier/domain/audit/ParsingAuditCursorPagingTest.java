package com.errorpurifier.domain.audit;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import com.errorpurifier.domain.audit.repository.ParsingAuditRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParsingAuditCursorPagingTest {

    private static final String ADMIN_TOKEN = "test-admin-token";
    private static final int TOTAL = 25;
    private static final int TIE_GROUPS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParsingAuditRepository auditRepository;

    @Autowired
    private ClientDeviceRepository deviceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareAuditLogsWithTiedTimestamps() {
        auditRepository.deleteAllInBatch();
        ClientDevice device = deviceRepository.save(ClientDevice.builder()
                .id(UUID.randomUUID())
                .pluginVersion("1.0.0-test")
                .build());
        for (int index = 0; index < TOTAL; index++) {
            auditRepository.save(ParsingAuditLog.builder()
                    .device(device)
                    .issueType(ParsingAuditLog.IssueType.PARSING_ERROR)
                    .rawLogContent("raw-" + index)
                    .parsedLogContent("parsed-" + index)
                    .userComment("paging-test")
                    .isMasked(false)
                    .build());
        }
        auditRepository.flush();

        // 같은 created_at 을 5건씩 공유하게 만들어 복합 정렬 키의 동점 상황을 강제한다.
        List<Long> ids = auditRepository.findAll().stream().map(ParsingAuditLog::getId).sorted().toList();
        for (int index = 0; index < ids.size(); index++) {
            int group = index / (TOTAL / TIE_GROUPS);
            jdbcTemplate.update("update parsing_audit_log set created_at = ? where id = ?",
                    java.sql.Timestamp.valueOf("2026-02-0" + (group + 1) + " 00:00:00"), ids.get(index));
        }
    }

    @Test
    void walksEveryRowExactlyOnceAcrossCursorPages() throws Exception {
        List<Long> collected = new ArrayList<>();
        String cursor = null;
        int requests = 0;

        do {
            JsonNode slice = requestSlice(cursor, 4);
            slice.get("items").forEach(item -> collected.add(item.get("id").asLong()));
            cursor = slice.get("nextCursor").isNull() ? null : slice.get("nextCursor").asText();
            requests++;
        } while (cursor != null && requests < TOTAL);

        List<Long> expected = auditRepository.findAll().stream()
                .map(ParsingAuditLog::getId)
                .sorted((left, right) -> Long.compare(right, left))
                .toList();

        assertThat(collected).hasSize(TOTAL);
        assertThat(collected).doesNotHaveDuplicates();
        // created_at 이 오래된 순으로 id 를 부여했으므로 created_at DESC, id DESC 는 id 내림차순과 같다.
        assertThat(collected).containsExactlyElementsOf(expected);
    }

    @Test
    void rejectsMalformedCursorWithConsistentErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/audit").header("X-Admin-Token", ADMIN_TOKEN).param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("cursor 값이 올바르지 않습니다."));
    }

    @Test
    void rejectsSizeOutsideAllowedRange() throws Exception {
        mockMvc.perform(get("/api/v1/audit").header("X-Admin-Token", ADMIN_TOKEN).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(get("/api/v1/audit").header("X-Admin-Token", ADMIN_TOKEN).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void rejectsNonNumericSizeWithConsistentErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/audit").header("X-Admin-Token", ADMIN_TOKEN).param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 파라미터 'size'의 형식이 올바르지 않습니다."));
    }

    private JsonNode requestSlice(String cursor, int size) throws Exception {
        var request = get("/api/v1/audit").header("X-Admin-Token", ADMIN_TOKEN)
                .param("size", String.valueOf(size));
        if (cursor != null) {
            request = request.param("cursor", cursor);
        }
        String body = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}

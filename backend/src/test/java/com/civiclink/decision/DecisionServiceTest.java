package com.civiclink.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DecisionServiceTest {
    @TempDir Path directory;

    @Test
    void normalizesFragmentsAndPersistsOriginalData() throws Exception {
        String json = "{\"response\":{\"resultCode\":\"00\",\"docMngNo\":1398,"
                + "\"dcsnYmd\":\"2021.00.00.\",\"reason\":{\"contents\":["
                + "{\"text\":\"1. 신청취지\\n장례비\"},{\"text\":\" 지급 요구\\n2. 피신청인 주장\\n기관 주장\\n3.사실관계\\n\"}]},"
                + "\"resultCn\":{\"contents\":{\"text\":\"지급 의견\"}},"
                + "\"object\":{\"footnote\":{\"index\":1,\"content\":\"각주\"}}}}";
        DecisionDto dto = DecisionService.normalize(new ObjectMapper().readTree(json), 1398, "복지노동민원");
        assertEquals("장례비 지급 요구", dto.requestContent);
        assertEquals("지급 의견", dto.decisionContent);
        assertEquals("2021.00.00.", dto.decisionDate);
        assertEquals(2, dto.warnings.size());
        com.civiclink.config.DatabaseConfig config = new com.civiclink.config.DatabaseConfig();
        DecisionRepository repository = new DecisionRepository(directory.toString(),
                config.jdbcTemplate(config.dataSource("jdbc:h2:mem:normalization;DB_CLOSE_DELAY=-1", "sa", "")));
        repository.save(dto);
        DecisionDto stored = repository.find(1398).orElseThrow();
        assertEquals(dto.rawResponse, stored.rawResponse);
        assertEquals(dto.body, stored.body);
        assertEquals("각주", stored.footnotes.path("content").asText());
    }
}

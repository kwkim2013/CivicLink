package com.civiclink.search;

import com.civiclink.decision.DecisionDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionSearchServiceTest {
    @Test void ranksByDirectionRatherThanMagnitude() {
        assertEquals(1, DecisionSearchService.cosine(new double[]{1, 0}, new double[]{10, 0}), 1e-9);
        assertEquals(0, DecisionSearchService.cosine(new double[]{1, 0}, new double[]{0, 1}), 1e-9);
        assertEquals(-1, DecisionSearchService.cosine(new double[]{1, 0}, new double[]{-1, 0}), 1e-9);
    }

    @Test void searchTextExcludesDecisionAndClassificationLabels() {
        DecisionDto dto = new DecisionDto();
        dto.title = "민원";
        dto.requestContent = "횡단보도 설치 요구";
        dto.decisionContent = "의견표명";
        dto.category = "경찰민원";
        assertEquals("민원\n횡단보도 설치 요구", DecisionSearchService.searchText(dto));
    }
}

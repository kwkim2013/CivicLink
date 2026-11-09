package com.civiclink.decision;

import com.civiclink.external.PublicDataClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;

@Service
public class DecisionCollectionService {
    private final PublicDataClient client;
    private final DecisionService service;
    private final DecisionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DecisionCollectionService(PublicDataClient client, DecisionService service,
                                     DecisionRepository repository) {
        this.client = client;
        this.service = service;
        this.repository = repository;
    }

    public CollectionResult collect(int page, int size, String from, String to, String search) {
        if (page < 1 || size < 1 || size > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageNo는 1 이상, numOfRows는 1~10이어야 합니다.");
        }
        try {
            LocalDate start = LocalDate.parse(from, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate end = LocalDate.parse(to, DateTimeFormatter.BASIC_ISO_DATE);
            if (start.isAfter(end)) throw new DateTimeParseException("date order", from, 0);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간을 yyyyMMdd 형식과 시작일 순서로 확인하세요.");
        }

        JsonNode response;
        try {
            response = mapper.readTree(client.getComplaintList(page, size, search, from, to)).path("response");
        } catch (IOException | RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "공공데이터 목록 조회에 실패했습니다.");
        }
        CollectionResult result = new CollectionResult();
        String code = response.path("resultCode").asText();
        if ("20".equals(code) && "NO_CONTENTS".equals(response.path("resultMsg").asText())) return result;
        if (!"00".equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "외부 API가 정상 목록을 반환하지 않았습니다.");
        }
        result.totalCount = response.path("totalCnt").asInt();
        JsonNode items = response.path("items").path("item");
        if (!items.isArray() && !items.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "목록의 items.item 형식을 확인하세요.");
        }
        Iterable<JsonNode> entries = items.isArray() ? items : Collections.singletonList(items);
        for (JsonNode item : entries) {
            if (result.fetched >= size) break;
            result.fetched++;
            long id = item.path("docMngNo").asLong(-1);
            if (id <= 0) {
                result.failed++;
                result.results.add(new CollectionResult.Entry(id, "FAILED", "문서번호가 올바르지 않습니다."));
                continue;
            }
            try {
                if (repository.find(id).isPresent()) {
                    result.skipped++;
                    result.results.add(new CollectionResult.Entry(id, "SKIPPED", "이미 저장된 문서입니다."));
                    continue;
                }
                service.collect(id, item.path("clsfNm").asText(""));
                result.saved++;
                result.results.add(new CollectionResult.Entry(id, "SAVED", "저장 완료"));
            } catch (IOException | ResponseStatusException e) {
                result.failed++;
                result.results.add(new CollectionResult.Entry(id, "FAILED", "상세 조회 또는 저장 실패. 개별 수집으로 재시도하세요."));
            }
        }
        return result;
    }
}

package com.civiclink.decision;

import com.civiclink.external.PublicDataClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientException;
import java.io.IOException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DecisionService {
    private final PublicDataClient client;
    private final DecisionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DecisionService(PublicDataClient client, DecisionRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public DecisionDto collect(long id, String category) {
        try {
            DecisionDto decision = normalize(mapper.readTree(client.getComplaintDetail(id)), id, category);
            repository.save(decision);
            return decision;
        } catch (RestClientException e) {
            // URI에 인증키가 포함될 수 있어 원본 예외 메시지는 클라이언트로 전달하지 않는다.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "공공데이터 상세 조회에 실패했습니다.");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결정례 해석 또는 저장에 실패했습니다.");
        }
    }

    public DecisionDto find(long id) {
        try {
            return repository.find(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 결정례가 없습니다."));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결정례 읽기에 실패했습니다.");
        }
    }

    public java.util.List<java.util.Map<String,Object>> list() {
        try {
            java.util.List<java.util.Map<String,Object>> result = new java.util.ArrayList<>();
            for (DecisionDto d : repository.findAll()) result.add(java.util.Map.of(
                    "documentNumber",d.documentNumber,"title",java.util.Objects.toString(d.title,""),
                    "category",java.util.Objects.toString(d.category,""),"decisionDate",java.util.Objects.toString(d.decisionDate,"")));
            return result;
        } catch(IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"결정례 목록 읽기에 실패했습니다.");
        }
    }

    static DecisionDto normalize(JsonNode root, long id, String category) {
        JsonNode response = root.path("response");
        if (!"00".equals(response.path("resultCode").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "외부 API가 정상 데이터를 반환하지 않았습니다.");
        }
        if (response.path("docMngNo").asLong(-1) != id) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "응답의 문서번호가 요청과 다릅니다.");
        }
        DecisionDto dto = new DecisionDto();
        dto.documentNumber = id;
        dto.title = response.path("bllNm").asText("");
        dto.category = category == null ? "" : category.trim();
        dto.decisionDate = response.path("dcsnYmd").asText("");
        dto.respondent = response.path("aplpr").asText("");
        dto.body = text(response.path("reason").path("contents"));
        dto.decisionContent = text(response.path("resultCn").path("contents"));
        dto.decisionType = response.path("resultTy").asText("");
        dto.footnotes = response.path("object").path("footnote");
        dto.rawResponse = root;
        dto.sourceUrl = "https://www.data.go.kr/data/15102383/openapi.do";
        dto.collectedAt = Instant.now().toString();
        Matcher request = Pattern.compile("(?s)1\\.\\s*신청취지\\s*(.*?)(?=\\n\\s*2\\.|\\z)").matcher(dto.body);
        dto.requestContent = request.find() ? request.group(1).trim() : "";
        if (dto.requestContent.isEmpty()) dto.warnings.add("신청취지를 자동 분리하지 못했습니다. 원문 확인이 필요합니다.");
        if (dto.body.matches("(?s).*3\\.\\s*사실관계\\s*$")) dto.warnings.add("본문이 사실관계 제목에서 끝납니다. 완전한 원문인지 확인하세요.");
        if (dto.decisionDate.contains("00")) dto.warnings.add("불완전한 결정일자를 원문 문자열로 보존했습니다.");
        if (dto.category.isEmpty()) dto.warnings.add("분류가 없습니다. 목록 응답의 clsfNm을 전달하세요.");
        return dto;
    }

    // API의 contents는 단일 객체 또는 배열이다. 각 조각을 순서대로 연결한다.
    private static String text(JsonNode contents) {
        if (contents.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode part : contents) result.append(text(part));
            return result.toString();
        }
        return contents.path("text").asText("");
    }
}

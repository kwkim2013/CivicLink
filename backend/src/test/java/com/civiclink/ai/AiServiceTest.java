package com.civiclink.ai;

import com.civiclink.config.DatabaseConfig;
import com.civiclink.decision.*;
import com.civiclink.search.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {
    @TempDir Path temp;
    @Test void citationsMustComeFromRetrievedDocumentsAndWeakMatchesAbstain() throws Exception {
        DatabaseConfig config=new DatabaseConfig();
        DecisionRepository repo=new DecisionRepository(temp.toString(),config.jdbcTemplate(config.dataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","")));
        DecisionDto doc=new DecisionDto();doc.documentNumber=1428;doc.title="횡단보도";doc.category="교통도로민원";doc.sourceUrl="https://www.data.go.kr/data/15102383/openapi.do";doc.requestContent="턱낮춤 요청";doc.decisionContent="개선 의견";repo.save(doc);
        DecisionSearchService search=new DecisionSearchService(repo,null,temp.resolve("unused.json").toString()) {
            @Override public List<Hit> search(String query,int count){return List.of(new Hit(0.7,doc));}
        };
        LlmClient valid=new LlmClient("http://localhost:1","fake") {
            @Override public JsonNode generate(String system,String input,Map<String,Object> schema){
                return new ObjectMapper().createObjectNode().put("answer","문서 1428을 참고하세요.").set("documentNumbers",new ObjectMapper().createArrayNode().add(1428));
            }
        };
        assertEquals(1,((List<?>)new AiService(search,repo,valid,0.55).answer("횡단보도 개선").get("references")).size());
        LlmClient invalid=new LlmClient("http://localhost:1","fake") {
            @Override public JsonNode generate(String system,String input,Map<String,Object> schema){return new ObjectMapper().createObjectNode().put("answer","허위 근거").set("documentNumbers",new ObjectMapper().createArrayNode().add(999));}
        };
        assertThrows(ResponseStatusException.class,()->new AiService(search,repo,invalid,0.55).answer("횡단보도 개선"));
        assertEquals("검토필요",new AiService(search,repo,invalid,0.9).classify("관련성 낮은 입력").get("category"));
    }
}

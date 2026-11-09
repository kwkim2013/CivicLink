package com.civiclink.ai;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class LlmClient {
    private final RestTemplate http; private final String url,model;
    private final ObjectMapper mapper=new ObjectMapper();
    public LlmClient(@Value("${llm.base-url:http://localhost:11434}") String url,@Value("${llm.model:qwen3:4b}") String model) {
        this.url=url.replaceAll("/+$","");this.model=model;
        SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(5000);f.setReadTimeout(180000);http=new RestTemplate(f);
    }
    public JsonNode generate(String system,String input,Map<String,Object> schema) {
        try {
            JsonNode r=http.postForObject(url+"/api/chat",Map.of("model",model,"stream",false,"think",false,"format",schema,
                "options",Map.of("temperature",0,"num_predict",1200),"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",input))),JsonNode.class);
            JsonNode result=mapper.readTree(r==null?"":r.path("message").path("content").asText());
            if(result==null||!result.isObject())throw new IllegalArgumentException();return result;
        }catch(RestClientException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"LLM 실행 실패. Ollama와 qwen3:4b 모델을 확인하세요.");}
        catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"LLM 응답을 JSON으로 해석할 수 없습니다.");}
    }
}

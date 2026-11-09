package com.civiclink.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class EmbeddingClient {
    private final RestTemplate http;
    private final String baseUrl;
    private final String model;

    public EmbeddingClient(@Value("${embedding.base-url:http://localhost:11434}") String baseUrl,
                           @Value("${embedding.model:bge-m3}") String model) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        this.http = new RestTemplate(factory);
    }

    public String model() { return model; }

    public double[] embed(String text) {
        try {
            JsonNode response = http.postForObject(baseUrl + "/api/embed",
                    Map.of("model", model, "input", text, "truncate", false), JsonNode.class);
            JsonNode vector = response == null ? null : response.path("embeddings").path(0);
            if (vector == null || !vector.isArray() || vector.size() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "유효한 임베딩 벡터가 없습니다.");
            }
            double[] values = new double[vector.size()];
            double norm = 0;
            for (int i = 0; i < values.length; i++) {
                if (!vector.get(i).isNumber()) throw new IllegalArgumentException();
                values[i] = vector.get(i).asDouble();
                if (!Double.isFinite(values[i])) throw new IllegalArgumentException();
                norm += values[i] * values[i];
            }
            if (norm == 0) throw new IllegalArgumentException();
            return values;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ollama 연결 또는 모델 실행 실패. Ollama 실행 및 bge-m3 모델 다운로드를 확인하세요.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "임베딩 벡터 값이 올바르지 않습니다.");
        }
    }
}

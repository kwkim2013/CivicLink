package com.civiclink.search;

import com.civiclink.decision.DecisionDto;
import com.civiclink.decision.DecisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class DecisionSearchService {
    private final DecisionRepository repository;
    private final EmbeddingClient embedding;
    private final Path indexFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public DecisionSearchService(DecisionRepository repository, EmbeddingClient embedding,
            @Value("${embedding.index-file:${civiclink.data-dir:C:/github/CivicLink/backend/data}/search-index.json}") String file) {
        this.repository = repository;
        this.embedding = embedding;
        this.indexFile = Paths.get(file).toAbsolutePath();
    }

    public static class Index {
        public String model;
        public List<VectorEntry> entries = new ArrayList<>();
    }
    public static class VectorEntry {
        public long documentNumber;
        public String text;
        public double[] vector;
    }
    public static class Hit {
        public double score;
        public DecisionDto decision;
        public Hit(double score, DecisionDto decision) { this.score = score; this.decision = decision; }
    }

    public synchronized Map<String, Object> buildIndex() {
        try {
            List<DecisionDto> documents = repository.findAll();
            if (documents.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "결정례를 먼저 수집하세요.");
            Index index = new Index();
            index.model = embedding.model();
            List<Long> skipped = new ArrayList<>();
            for (DecisionDto doc : documents) {
                if (doc.requestContent == null || doc.requestContent.isBlank()) {
                    skipped.add(doc.documentNumber);
                    continue;
                }
                VectorEntry entry = new VectorEntry();
                entry.documentNumber = doc.documentNumber;
                entry.text = searchText(doc);
                entry.vector = embedding.embed(entry.text);
                index.entries.add(entry);
            }
            if (index.entries.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "신청취지가 있는 결정례가 없습니다.");
            Files.createDirectories(indexFile.getParent());
            Path temporary = Files.createTempFile(indexFile.getParent(), "index-", ".tmp");
            try {
                mapper.writeValue(temporary.toFile(), index);
                try {
                    Files.move(temporary, indexFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, indexFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
            return Map.of("model", index.model, "indexed", index.entries.size(), "skipped", skipped);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "검색 인덱스 저장에 실패했습니다.");
        }
    }

    public List<Hit> search(String query, int topK) {
        if (query == null || query.isBlank() || query.length() > 4000 || topK < 1 || topK > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query는 1~4000자, topK는 1~10으로 입력하세요.");
        }
        if (!Files.exists(indexFile)) throw new ResponseStatusException(HttpStatus.CONFLICT, "임베딩 인덱스를 먼저 생성하세요.");
        try {
            Index index = mapper.readValue(indexFile.toFile(), Index.class);
            if (!embedding.model().equals(index.model)) throw new ResponseStatusException(HttpStatus.CONFLICT, "모델이 변경됐습니다. 인덱스를 재생성하세요.");
            double[] vector = embedding.embed(query);
            List<Hit> hits = new ArrayList<>();
            for (VectorEntry entry : index.entries) {
                Optional<DecisionDto> doc = repository.find(entry.documentNumber);
                if (doc.isEmpty()) continue;
                if (!searchText(doc.get()).equals(entry.text) || entry.vector.length != vector.length) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "원문 또는 벡터 크기가 변경됐습니다. 인덱스를 재생성하세요.");
                }
                hits.add(new Hit(cosine(vector, entry.vector), doc.get()));
            }
            hits.sort(Comparator.comparingDouble((Hit hit) -> hit.score).reversed());
            return new ArrayList<>(hits.subList(0, Math.min(topK, hits.size())));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "검색 인덱스를 읽을 수 없습니다.");
        }
    }

    static String searchText(DecisionDto doc) {
        return Objects.toString(doc.title, "") + "\n" + Objects.toString(doc.requestContent, "");
    }

    static double cosine(double[] left, double[] right) {
        if (left.length != right.length || left.length == 0) throw new IllegalArgumentException("vector dimensions");
        double dot = 0, a = 0, b = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i]; a += left[i] * left[i]; b += right[i] * right[i];
        }
        return a == 0 || b == 0 ? 0 : dot / (Math.sqrt(a) * Math.sqrt(b));
    }
}


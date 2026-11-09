package com.civiclink.decision;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

@Repository
public class DecisionRepository {
    private final Path directory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbc;

    public DecisionRepository(@Value("${publicdata.storage-dir:${civiclink.data-dir:C:/github/CivicLink/backend/data}/decisions}") String directory,
                              JdbcTemplate jdbc) throws IOException {
        this.directory = Paths.get(directory);
        this.jdbc = jdbc;
        // 기존 JSON은 보존하고 미이관 문서만 DB에 추가한다.
        if (Files.isDirectory(this.directory)) {
            try (Stream<Path> files = Files.list(this.directory)) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.getFileName().toString().matches("[0-9]+\\.json"))::iterator) {
                    DecisionDto dto = mapper.readValue(file.toFile(), DecisionDto.class);
                    jdbc.update("INSERT INTO decisions(id,title,category,payload) SELECT ?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM decisions WHERE id=?)",
                            dto.documentNumber, dto.title, dto.category, mapper.writeValueAsString(dto), dto.documentNumber);
                }
            }
        }
    }

    public void save(DecisionDto decision) throws IOException {
        jdbc.update("MERGE INTO decisions(id,title,category,payload) KEY(id) VALUES(?,?,?,?)",
                decision.documentNumber, decision.title, decision.category, mapper.writeValueAsString(decision));
    }

    public Optional<DecisionDto> find(long id) throws IOException {
        List<String> rows = jdbc.query("SELECT payload FROM decisions WHERE id=?", (rs, n) -> rs.getString(1), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(mapper.readValue(rows.get(0), DecisionDto.class));
    }

    public List<DecisionDto> findAll() throws IOException {
        List<DecisionDto> results = new ArrayList<>();
        for (String payload : jdbc.query("SELECT payload FROM decisions ORDER BY id", (rs, n) -> rs.getString(1)))
            results.add(mapper.readValue(payload, DecisionDto.class));
        return results;
    }
}


package com.civiclink.search;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/decisions")
public class DecisionSearchController {
    private final DecisionSearchService service;
    public DecisionSearchController(DecisionSearchService service) { this.service = service; }

    @PostMapping("/index")
    public Map<String, Object> index() { return service.buildIndex(); }

    public static class SearchRequest {
        public String query;
        public int topK = 3;
    }

    @PostMapping("/search")
    public List<DecisionSearchService.Hit> search(@RequestBody SearchRequest request) {
        return service.search(request.query, request.topK);
    }
}

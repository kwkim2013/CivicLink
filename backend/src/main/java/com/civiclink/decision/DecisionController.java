package com.civiclink.decision;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decisions")
public class DecisionController {
    private final DecisionService service;
    private final DecisionCollectionService collectionService;

    public DecisionController(DecisionService service, DecisionCollectionService collectionService) {
        this.service = service;
        this.collectionService = collectionService;
    }

    @PostMapping("/collect")
    public CollectionResult collectPage(
            @RequestParam(name = "pageNo", defaultValue = "1") int page,
            @RequestParam(name = "numOfRows", defaultValue = "10") int size,
            @RequestParam(name = "reqFrom") String from,
            @RequestParam(name = "reqTo") String to,
            @RequestParam(name = "search", defaultValue = "") String search) {
        return collectionService.collect(page, size, from, to, search);
    }

    @PostMapping("/{docMngNo}/collect")
    public DecisionDto collect(@PathVariable("docMngNo") long id,
                               @RequestParam(name = "category", defaultValue = "") String category) {
        return service.collect(id, category);
    }

    @GetMapping("/{docMngNo}")
    public DecisionDto find(@PathVariable("docMngNo") long id) {
        return service.find(id);
    }

    @GetMapping
    public java.util.List<java.util.Map<String,Object>> list() { return service.list(); }
}

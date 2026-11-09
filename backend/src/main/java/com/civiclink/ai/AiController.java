package com.civiclink.ai;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;
    public AiController(AiService service){this.service=service;}
    public static class Input{public String content;}
    @PostMapping("/answer")public Map<String,Object> answer(@RequestBody Input input){return service.answer(input.content);}
    @PostMapping("/classify")public Map<String,Object> classify(@RequestBody Input input){return service.classify(input.content);}
}

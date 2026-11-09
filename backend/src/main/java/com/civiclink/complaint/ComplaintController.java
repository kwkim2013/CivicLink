package com.civiclink.complaint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import com.civiclink.auth.ApiAccessInterceptor;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository repository;
    public ComplaintController(ComplaintRepository repository) { this.repository=repository; }

    @GetMapping
    public List<Complaint> findAll(HttpServletRequest req) {
        return repository.list(ApiAccessInterceptor.user(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> findById(@PathVariable("id") Long id,HttpServletRequest req) {
        Complaint complaint = repository.find(id,ApiAccessInterceptor.user(req));
        if (complaint == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(complaint);
    }

    @PostMapping
    public ResponseEntity<Complaint> create(@RequestBody Complaint request,HttpServletRequest req) {
        validate(request);
        Complaint complaint = repository.create(request,ApiAccessInterceptor.user(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(complaint);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,HttpServletRequest req) {
        if (repository.delete(id,ApiAccessInterceptor.user(req)) == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Complaint> update(@PathVariable("id") Long id,@RequestBody Complaint input,HttpServletRequest req) {
        validate(input);
        if(repository.update(id,input,ApiAccessInterceptor.user(req))==0)return ResponseEntity.notFound().build();
        return findById(id,req);
    }
    private void validate(Complaint input) {
        if(input.getTitle()==null || input.getTitle().isBlank() || input.getTitle().length()>300 ||
                input.getContent()==null || input.getContent().isBlank() || input.getContent().length()>4000 ||
                (input.getCategory()!=null && input.getCategory().length()>100))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"제목은 1~300자, 내용은 1~4000자, 분류는 100자 이내로 입력하세요.");
    }
}

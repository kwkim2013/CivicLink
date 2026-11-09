package com.civiclink.config;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,Object>> status(ResponseStatusException e){
        if (e.getStatus().is4xxClientError()) log.warn("API {}: {}", e.getStatus().value(), e.getReason());
        return error(e.getStatus(),e.getReason()==null?"요청 처리 실패":e.getReason());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MissingServletRequestParameterException.class,MethodArgumentTypeMismatchException.class})public ResponseEntity<Map<String,Object>> badRequest(Exception e){
        log.warn("API 400: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST,"요청 JSON 및 파라미터 형식을 확인하세요.");
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)public ResponseEntity<Map<String,Object>> method(Exception e){
        log.warn("API 405: {}", e.getMessage());
        return error(HttpStatus.METHOD_NOT_ALLOWED,"HTTP 요청 메소드를 확인하세요.");
    }
    @ExceptionHandler(DataAccessException.class)public ResponseEntity<Map<String,Object>> database(Exception e){
        String id = UUID.randomUUID().toString();
        log.error("API 500 errorId=" + id + " Database operation failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status",500,"message","DB 작업에 실패했습니다.","errorId",id));
    }
    @ExceptionHandler(org.springframework.web.client.RestClientException.class)public ResponseEntity<Map<String,Object>> external(Exception e){return error(HttpStatus.BAD_GATEWAY,"외부 API 호출에 실패했습니다. 인증키와 요청 조건을 확인하세요.");}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> unexpected(Exception e){
        log.error("Unexpected API error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,"서버 내부 오류가 발생했습니다.");
    }
    private ResponseEntity<Map<String,Object>> error(HttpStatus status,String message){return ResponseEntity.status(status).body(Map.of("status",status.value(),"message",message,"errorId",UUID.randomUUID().toString()));}
}

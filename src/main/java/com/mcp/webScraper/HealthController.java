package com.mcp.webScraper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<?> health() {
        return new ResponseEntity<>(Map.of("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "version", "1.0.1"), HttpStatus.OK);
    }
}

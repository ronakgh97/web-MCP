package com.mcp.webScraper.Controllers;

import com.mcp.webScraper.Services.ScrapeServices;
import com.mcp.webScraper.Services.SearchServices;
import com.mcp.webScraper.entity.RequestEntries;
import com.mcp.webScraper.entity.ResponseEntries;
import com.mcp.webScraper.entity.ScrapeResult;
import com.mcp.webScraper.entity.SearchResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/service")
public class ServiceController { //TODO: clean up and centralize exception handling

    private static final Logger log = LoggerFactory.getLogger(ServiceController.class);

    @Autowired
    private ScrapeServices scrapeServices;

    @Autowired
    private SearchServices searchServices;

    @PostMapping("/search")
    public ResponseEntity<ResponseEntries> search(@Valid @RequestBody RequestEntries request) {
        long startTime = System.currentTimeMillis();
        ResponseEntries response = new ResponseEntries();

        try {
            List<SearchResult> results = searchServices.performSearch(
                    request.getRequestId(),
                    request.getQuery(),
                    request.getResults()
            );

            if (results.isEmpty()) {
                response.setSuccess(false);
                response.setUserQuery(request.getQuery());
                response.setMessage("No search results");
                response.setSearchResultList(results); //Exception handled in other classes
                response.setExecutionTimeMs(startTime);

                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

            response.setSuccess(true);
            response.setUserQuery(request.getQuery());
            response.setMessage("Search results found");
            for (SearchResult result : results) {
                ScrapeResult scrapeResult = scrapeServices.scrapeContent(request.getRequestId(), result.getSource());
                result.setContent(scrapeResult.getContent());
            }
            response.setSearchResultList(results);
            response.addExecutionTime(startTime);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setUserQuery(request.getQuery());
            response.setMessage("Something went wrong");
            response.setSearchResultList(null);
            response.addExecutionTime(startTime);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean healthy = searchServices.isServiceHealthy() && scrapeServices.isServiceHealthy();
        log.info("Status: {}", healthy);
        if (healthy)
            return new ResponseEntity<>(Map.of("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), "health: ", 1), HttpStatus.OK);
        else
            return new ResponseEntity<>(Map.of("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), "health: ", 0), HttpStatus.SERVICE_UNAVAILABLE);
    }
}

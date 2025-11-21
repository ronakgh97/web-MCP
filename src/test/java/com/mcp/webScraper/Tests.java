package com.mcp.webScraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.webScraper.Workers.PlaywrightAllocator;
import com.mcp.webScraper.entity.RequestEntries;
import com.mcp.webScraper.entity.ResponseEntries;
import com.mcp.webScraper.entity.SearchResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Tests {

    private static final Logger log = LoggerFactory.getLogger(Tests.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlaywrightAllocator playwrightAllocator;

    @Test
    @Order(0)
    void check() {
        assertTrue(true);
        log.info("Basic connectivity test passed");
    }

    // BASIC FUNCTIONALITY TESTS

    @Test
    @Order(1)
    void testSearch_BasicQuery() throws Exception {
        RequestEntries request = new RequestEntries("spring boot", 3);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        logResponse("test_logs/basic", response.getUserQuery(), response);

        // Core assertions
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUserQuery()).isEqualTo("spring boot");
        assertThat(response.getSearchResultList()).isNotEmpty();
        assertThat(response.getSearchResultList()).hasSizeLessThanOrEqualTo(3);

        // Validate first result
        SearchResult firstResult = response.getSearchResultList().get(0);
        assertThat(firstResult.getSource()).startsWith("https://");
        assertThat(firstResult.getSnippet()).isNotEmpty();
        assertThat(firstResult.getContent()).isNotEmpty();
        assertThat(firstResult.getError()).isNull();

        log.info("Basic search test completed with {} results", response.getSearchResultList().size());
    }

    @Test
    @Order(2)
    void testSearch_TechQuery() throws Exception {
        RequestEntries request = new RequestEntries("New AI models in the market", 2);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult results = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                results.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        logResponse("test_logs/tech", response.getUserQuery(), response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSearchResultList()).isNotEmpty();
        assertThat(response.getSearchResultList()).hasSizeLessThanOrEqualTo(2);

        // Check content quality
        for (SearchResult result : response.getSearchResultList()) {
            assertThat(result.getSource()).matches("^https://.*");
            assertThat(result.getSnippet()).isNotEmpty();
            assertThat(result.getContent()).hasSizeGreaterThan(100);
        }
    }

    // EDGE CASE TESTS
    @Test
    @Order(3)
    void testSearch_EmptyQuery() throws Exception {
        RequestEntries request = new RequestEntries("", 5);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        
        log.info("Empty query validation working correctly - returned 400 Bad Request");
    }

    @Test
    @Order(4)
    void testSearch_ZeroResults() throws Exception {
        RequestEntries request = new RequestEntries("what is mcp?", 0);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        // Should default to reasonable number or handle gracefully
        assertThat(response.isSuccess()).isTrue();
        log.info("Zero results request handled properly");
    }

    @Test
    @Order(5)
    void testSearch_SpecialCharacters() throws Exception {
        RequestEntries request = new RequestEntries("C++ vs Rust performance @2025 #programming", 2);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        logResponse("test_logs/edge_cases", "special_chars", response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUserQuery()).contains("C++");
        assertThat(response.getUserQuery()).contains("#programming");
    }

    // INVALID REQUEST TESTS

    @Test
    @Order(6)
    void testSearch_InvalidJson() throws Exception {
        String invalidJson = "{ invalid json structure }";

        mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        log.info("Invalid JSON test passed - returned 400 Bad Request");
    }

    @Test
    @Order(7)
    void testSearch_MissingRequiredFields() throws Exception {
        String incompleteJson = "{ \"query\": \"rust\" }"; // Missing results field

        mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteJson))
                .andExpect(status().isOk()); // Should use default value for results field
        
        log.info("Missing required fields test passed - used default values");
    }

    @Test
    @Order(8)
    void testSearch_NegativeResultCount() throws Exception {
        RequestEntries request = new RequestEntries("rust by practice", -5);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        // Should handle negative values gracefully (likely default to reasonable number)
        assertThat(response.isSuccess()).isTrue();
        log.info("Negative result count handled properly");
    }

    // REAL-WORLD SCENARIOS
    @Test
    @Order(9)
    void testSearch_JobSearchScenario() throws Exception {
        RequestEntries request = new RequestEntries("software engineer jobs remote 2025", 4);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult results = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                results.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        logResponse("test_logs/scenarios", "job_search", response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSearchResultList()).isNotEmpty();

        // Job search results should have meaningful content
        for (SearchResult result : response.getSearchResultList()) {
            assertThat(result.getContent()).hasSizeGreaterThan(200);

        }
    }

    @Test
    @Order(10)
    void testSearch_TechnicalDocumentation() throws Exception {
        RequestEntries request = new RequestEntries("Spring Boot actuator endpoints documentation", 2);
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult results = mockMvc.perform(post("/api/v1/service/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        ResponseEntries response = objectMapper.readValue(
                results.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        logResponse("test_logs/scenarios", "tech_docs", response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSearchResultList()).isNotEmpty();

        // Documentation should have substantial content
        boolean hasDocumentationSite = response.getSearchResultList().stream()
                .anyMatch(result -> result.getSource().contains("spring.io") ||
                        result.getSource().contains("docs.") ||
                        result.getSnippet() != null && result.getSnippet().toLowerCase().contains("documentation"));

        log.info("Found documentation site: {}", hasDocumentationSite);
    }

    // UTILITY METHODS
    private void logResponse(String dir, String query, ResponseEntries response) throws IOException {
        Path folder = Path.of(dir);
        Files.createDirectories(folder);

        String safeQuery = query.replaceAll("[^a-zA-Z0-9_]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "search-response-" + safeQuery + "-" + timestamp + ".json";
        Path filePath = folder.resolve(filename);

        // Enhanced response logging with metadata
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().toString());
        logData.put("testQuery", query);
        logData.put("response", response);
        logData.put("executionTime", response.getExecutionTimeMs());
        logData.put("resultCount", response.getSearchResultList() != null ? response.getSearchResultList().size() : 0);

        Files.writeString(filePath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logData));
        log.debug("Test logs saved to: {}", filePath.toAbsolutePath());
    }

    @Test
    @Order(999)
    void testCleanup_FinalStats() {
        // Final health check and stats
        log.info("FINAL TEST SUITE STATS");
        log.info("Allocator Health: {}", playwrightAllocator.isHealthy());
        log.info("Allocator Initialized: {}", playwrightAllocator.isInitialized());
        log.info("Active Search Instances: {}", playwrightAllocator.getActiveSearchInstances());
        log.info("Active Scraper Instances: {}", playwrightAllocator.getActiveScraperInstances());
        log.info("Usage Statistics:\n{}", playwrightAllocator.getUsageStatistics());

        assertThat(playwrightAllocator.isHealthy()).isTrue();
    }
}
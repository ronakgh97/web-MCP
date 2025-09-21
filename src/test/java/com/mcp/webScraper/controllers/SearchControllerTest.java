package com.mcp.webScraper.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.webScraper.entries.ResponseEntries;
import com.mcp.webScraper.entries.SearchResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    private static final Logger log = LoggerFactory.getLogger(SearchControllerTest.class);
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void check() {
        assertTrue(true);
    }

    @Test
    void testSearchEndpoint() throws Exception {
        // Prepare request body
        String requestJson = "{\"query\":\"video2x github\"}";

        // Perform POST request
        MvcResult result = mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        // Deserialize response directly into DTO
        ResponseEntries response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ResponseEntries.class
        );

        Path folder = Path.of("test_logs");
        Files.createDirectories(folder);

        String safeQuery = response.getUserQuery().replaceAll("[^a-zA-Z0-9]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = "search-response-" + safeQuery + "-" + timestamp + ".json";
        Path filePath = folder.resolve(filename);
        Files.writeString(filePath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        log.debug("Test logs saved to: {}", filePath.toAbsolutePath());

        // Assertions
        assertThat(response.getUserQuery()).isEqualTo("video2x github");
        assertThat(response.getSearchResultList()).isNotEmpty();

        SearchResult firstResult = response.getSearchResultList().get(0);
        assertTrue(firstResult.isSuccess());
        assertThat(firstResult.getSource()).startsWith("https://");
        assertThat(firstResult.getSnippet()).isNotEmpty();
        assertThat(firstResult.getContent()).isNotEmpty();
        assertThat(firstResult.getError()).isNull();
    }
}
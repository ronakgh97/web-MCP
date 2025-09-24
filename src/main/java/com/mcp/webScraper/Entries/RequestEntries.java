package com.mcp.webScraper.Entries;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Request data transfer object for web search operations.
 * Contains user query and search configuration parameters.
 */
public class RequestEntries {

    private int requestId;

    @NotBlank(message = "Query cannot be empty")
    @Size(min = 1, max = 800, message = "Query must be between 1 and 500 characters")
    @JsonProperty("query")
    private String query;

    @Min(value = 1, message = "Results count must be at least 1")
    @Max(value = 10, message = "Results count cannot exceed 10")
    @JsonProperty("results")
    private int results = 3; // Default

    public RequestEntries() {
        this.requestId = generateRequestId();
    }

    public RequestEntries(@NotNull String query, int results) {
        this.requestId = generateRequestId();
        this.query = query;
        this.results = Math.max(1, Math.min(10, results));
    }

    // Generate unique request ID
    private int generateRequestId() {
        int hash = UUID.randomUUID().hashCode();
        return hash & 0x7fffffff; // Ensure positive integer
    }

    // Getters and Setters

    public int getRequestId() {
        return requestId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query != null ? query.trim() : null;
    }

    public int getResults() {
        return results;
    }

    public void setResults(int results) {
        this.results = Math.max(1, Math.min(10, results));
    }

    // Utility methods
    @Override
    public String toString() {
        return String.format("RequestEntries{requestId=%d, query='%s', results=%d}",
                requestId, query, results);
    }
}
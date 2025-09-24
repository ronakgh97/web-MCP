package com.mcp.webScraper.Entries;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Response data transfer object for web search operations.
 * Contains search results, metadata, and operation status.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseEntries {

    private int responseId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("userQuery")
    private String userQuery;

    @JsonProperty("searchResultList")
    private List<SearchResult> searchResultList;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    @JsonProperty("resultCount")
    private int resultCount;

    public ResponseEntries() {
        this.responseId = generateResponseId();
    }

    public ResponseEntries(String message, String userQuery, List<SearchResult> searchResultList, boolean success) {
        this.responseId = generateResponseId();
        this.message = message;
        this.userQuery = userQuery;
        this.searchResultList = searchResultList;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.success = success;
        this.resultCount = this.searchResultList.size();
    }

    // Generate unique response ID
    private int generateResponseId() {
        int hash = UUID.randomUUID().hashCode();
        return hash & 0x7fffffff;
    }

    // Getters and Setters

    public int getResponseId() {
        return responseId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public List<SearchResult> getSearchResultList() {
        return searchResultList;
    }

    public void setSearchResultList(List<SearchResult> searchResultList) {
        this.searchResultList = searchResultList != null ? searchResultList : new ArrayList<>();
        this.resultCount = this.searchResultList.size();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    // ✅ Utility methods

    public void addExecutionTime(long startTime) {
        this.executionTimeMs = System.currentTimeMillis() - startTime;
    }

    public boolean hasResults() {
        return searchResultList != null && !searchResultList.isEmpty();
    }

    public void markAsError(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
        if (this.searchResultList == null) {
            this.searchResultList = new ArrayList<>();
        }
        this.resultCount = this.searchResultList.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResponseEntries that = (ResponseEntries) obj;
        return responseId == that.responseId &&
                success == that.success &&
                Objects.equals(message, that.message) &&
                Objects.equals(userQuery, that.userQuery) &&
                Objects.equals(searchResultList, that.searchResultList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseId, message, userQuery, searchResultList, success);
    }

    @Override
    public String toString() {
        return String.format("ResponseEntries{responseId=%d, success=%s, userQuery='%s', resultCount=%d}",
                responseId, success, userQuery, resultCount);
    }
}

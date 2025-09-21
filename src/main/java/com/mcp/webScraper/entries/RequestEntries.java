package com.mcp.webScraper.entries;

import java.util.UUID;

public class RequestEntries {

    private long requestId;

    private String query;

    private int results;

    public RequestEntries() {
    }

    public RequestEntries(long requestId, String query, int results) {
        this.requestId = requestId;
        this.query = query;
        this.results = results;
    }

    public int getRequestId() {
        int hash = UUID.randomUUID().hashCode();
        hash = hash & 0x7fffffff; // mask the sign bit
        return hash;

    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getResults() {
        return results;
    }

    public void setResults(int results) {
        this.results = results;
    }
}

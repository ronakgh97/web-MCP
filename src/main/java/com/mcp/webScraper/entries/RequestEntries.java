package com.mcp.webScraper.entries;

public class RequestEntries {

    private String query;

    private int results;

    public RequestEntries() {
    }

    public RequestEntries(String query, int results) {
        this.query = query;
        this.results = results;
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

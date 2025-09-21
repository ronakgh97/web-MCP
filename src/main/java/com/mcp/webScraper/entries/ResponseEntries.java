package com.mcp.webScraper.entries;

import java.util.List;
import java.util.UUID;

public class ResponseEntries {

    private long responseId;

    private String Message;

    private String userQuery;

    private List<SearchResult> searchResultList;

    public ResponseEntries() {
    }

    public ResponseEntries(long responseId, String message, String userQuery, List<SearchResult> searchResultList) {
        Message = message;
        this.userQuery = userQuery;
        this.searchResultList = searchResultList;
    }

    public int getResponseId() {
        int hash = UUID.randomUUID().hashCode();
        hash = hash & 0x7fffffff; // mask the sign bit
        return hash;

    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
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
        this.searchResultList = searchResultList;
    }
}

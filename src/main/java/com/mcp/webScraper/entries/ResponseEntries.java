package com.mcp.webScraper.entries;

import java.util.List;

public class ResponseEntries {

    private String Message;

    private String userQuery;

    private List<SearchResult> searchResultList;

    public ResponseEntries() {
    }

    public ResponseEntries(String message, String userQuery, List<SearchResult> searchResultList) {
        Message = message;
        this.userQuery = userQuery;
        this.searchResultList = searchResultList;
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

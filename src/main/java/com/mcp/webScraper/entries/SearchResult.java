package com.mcp.webScraper.entries;

public class SearchResult {
    private String source;

    private String content;

    public SearchResult() {
    }

    public SearchResult(String link, String content) {
        this.source = link;
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String link) {
        this.source = link;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

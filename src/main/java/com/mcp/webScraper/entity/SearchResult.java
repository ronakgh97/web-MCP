package com.mcp.webScraper.entity;

public class SearchResult {

    private boolean success;

    private String source;

    private String snippet;

    private String content;

    private String error;

    public SearchResult() {
    }

    public SearchResult(boolean success, String link, String snippet, String content, String error) {
        this.success = success;
        this.source = link;
        this.snippet = snippet;
        this.content = content;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String link) {
        this.source = link;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "success=" + success +
                ", source=" + source +
                ", snippet='" + snippet + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(100, content.length())) + "..." : null) + '\'' +
                ", error='" + error +
                '}';
    }
}

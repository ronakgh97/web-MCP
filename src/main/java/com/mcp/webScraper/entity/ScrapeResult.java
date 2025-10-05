package com.mcp.webScraper.entity;

public class ScrapeResult {

    private boolean success;

    private String url;

    private String content;

    private String error;

    public ScrapeResult() {
    }

    public ScrapeResult(boolean success, String content, String url, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
        this.url = url;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ScrapeResult{" +
                "success=" + success +
                ", url='" + url + '\'' +
                ", error='" + error + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(100, content.length())) + "..." : null) + '\'' +
                '}';
    }
}


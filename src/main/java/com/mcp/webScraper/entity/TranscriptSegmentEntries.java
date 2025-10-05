package com.mcp.webScraper.entity;

public class TranscriptSegmentEntries {
    private String timestamp;
    private String text;

    public TranscriptSegmentEntries() {
    }

    public TranscriptSegmentEntries(String timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}


package com.mcp.webScraper.entity;

import java.util.List;

public class TranscriptResult {
    private String videoId;
    private String title;
    private String url;
    private String fullTranscript;
    private List<TranscriptSegmentEntries> segments;
    private boolean success;

    public TranscriptResult() {
    }

    public TranscriptResult(String videoId, String title, String url,
                            String fullTranscript, List<TranscriptSegmentEntries> segments,
                            boolean success) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
        this.fullTranscript = fullTranscript;
        this.segments = segments;
        this.success = success;
    }

    // Getters and setters
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFullTranscript() { return fullTranscript; }
    public void setFullTranscript(String fullTranscript) { this.fullTranscript = fullTranscript; }

    public List<TranscriptSegmentEntries> getSegments() { return segments; }
    public void setSegments(List<TranscriptSegmentEntries> segments) { this.segments = segments; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}


package com.mcp.webScraper.entity;

public class YoutubeVideoResultEntries {
    private String url;
    private String title;
    private String channel;
    private String views;
    private String duration;
    private String thumbnail;

    public YoutubeVideoResultEntries() {
    }

    public YoutubeVideoResultEntries(String url, String title, String channel,
                                     String views, String duration, String thumbnail) {
        this.url = url;
        this.title = title;
        this.channel = channel;
        this.views = views;
        this.duration = duration;
        this.thumbnail = thumbnail;
    }

    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getViews() { return views; }
    public void setViews(String views) { this.views = views; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
}


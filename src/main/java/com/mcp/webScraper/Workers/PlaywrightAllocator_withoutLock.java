package com.mcp.webScraper.Workers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("dev")
@DependsOn("proxyService")
@Service
public class PlaywrightAllocator_withoutLock {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightAllocator_withoutLock.class);

    private PlaywrightBrowserSearchTools[] searchTools;
    private PlaywrightWebScraperTools[] scraperTools;

    @Value("${playwright.instances:10}")
    private int instances;

    private volatile boolean initialized = false;

    @PostConstruct
    private void init() {
        log.info("Initializing {} Playwright instances...", instances);
        allocate();
    }

    private void allocate() {
        try {
            searchTools = new PlaywrightBrowserSearchTools[instances];
            scraperTools = new PlaywrightWebScraperTools[instances];

            // Initialize search instances
            for (int i = 0; i < instances; i++) {
                try {
                    searchTools[i] = new PlaywrightBrowserSearchTools();
                    log.debug("Search instance {} initialized", i);
                } catch (Exception e) {
                    log.error("Failed to initialize search instance {}: {}", i, e.getMessage());
                    searchTools[i] = null; // Mark as failed
                }
            }

            // Initialize scraper instances
            for (int i = 0; i < instances; i++) {
                try {
                    scraperTools[i] = new PlaywrightWebScraperTools();
                    log.debug("Scraper instance {} initialized", i);
                } catch (Exception e) {
                    log.error("Failed to initialize scraper instance {}: {}", i, e.getMessage());
                    scraperTools[i] = null; // Mark as failed
                }
            }

            initialized = true;
            log.info("Playwright allocator initialized successfully with {} instances", instances);

        } catch (Exception e) {
            log.error("Critical failure during instance allocation: {}", e.getMessage());
            initialized = false;
        }
    }

    @Deprecated
    public PlaywrightBrowserSearchTools getSearchInstance_withoutLock(int id) {
        if (!initialized || searchTools == null) {
            log.warn("Allocator not initialized - cannot provide search instance");
            return null;
        }

        int index = Math.abs(id % instances);
        PlaywrightBrowserSearchTools instance = searchTools[index];

        if (instance == null) {
            // Fallback to next available instance
            for (int i = 0; i < instances; i++) {
                int fallbackIndex = (index + i) % instances;
                if (searchTools[fallbackIndex] != null) {
                    log.debug("Using fallback search instance {} for request {}", fallbackIndex, id);
                    return searchTools[fallbackIndex];
                }
            }
            log.error("No healthy search instances available!");
            return null;
        }
        return instance;
    }

    @Deprecated
    public PlaywrightWebScraperTools getScraperInstance_withoutLock(int id) {
        if (!initialized || scraperTools == null) {
            log.warn("Allocator not initialized - cannot provide scraper instance");
            return null;
        }

        int index = Math.abs(id % instances);
        PlaywrightWebScraperTools instance = scraperTools[index];

        if (instance == null) {
            // Fallback to next available instance
            for (int i = 0; i < instances; i++) {
                int fallbackIndex = (index + i) % instances;
                if (scraperTools[fallbackIndex] != null) {
                    log.debug("Using fallback scraper instance {} for request {}", fallbackIndex, id);
                    return scraperTools[fallbackIndex];
                }
            }
            log.error("No healthy scraper instances available!");
            return null;
        }

        return instance;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getInstanceCount() {
        return instances;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up Playwright instances...");

        if (searchTools != null) {
            for (int i = 0; i < searchTools.length; i++) {
                if (searchTools[i] != null) {
                    try {
                        searchTools[i].cleanup();
                    } catch (Exception e) {
                        log.warn("Error cleaning search instance {}: {}", i, e.getMessage());
                    }
                }
            }
        }

        if (scraperTools != null) {
            for (int i = 0; i < scraperTools.length; i++) {
                if (scraperTools[i] != null) {
                    try {
                        scraperTools[i].cleanup();
                    } catch (Exception e) {
                        log.warn("Error cleaning scraper instance {}: {}", i, e.getMessage());
                    }
                }
            }
        }

        log.info("Playwright cleanup completed");
    }
}

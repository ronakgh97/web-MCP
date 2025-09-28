package com.mcp.webScraper.Services;

import com.mcp.webScraper.Entries.ScrapeResult;
import com.mcp.webScraper.Workers.PlaywrightAllocator;
import com.mcp.webScraper.Workers.PlaywrightAllocator_withoutLock;
import com.mcp.webScraper.Workers.PlaywrightWebScraperTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
public class ScrapeServices {

    private static final Logger log = LoggerFactory.getLogger(ScrapeServices.class);

    @Autowired(required = false)
    private PlaywrightAllocator playwrightAllocator;

    @Autowired(required = false)
    private PlaywrightAllocator_withoutLock playwrightAllocatorWithoutLock;

    @Cacheable(value = "scrapedPages", key = "#url")
    public ScrapeResult scrapeContent(int requestId, String url) {
        // Input validation
        if (url == null || url.trim().isEmpty()) {
            log.warn("Empty URL provided for scraping request {}", requestId);
            return createErrorResult(url, "Empty URL provided");
        }

        PlaywrightWebScraperTools scrapeTool = null;

        try {
            // Borrow instance
            scrapeTool = playwrightAllocator.borrowScraperInstance(requestId);
            if (scrapeTool == null) {
                log.error("No scraper instance available for request {}", requestId);
                return createErrorResult(url, "Scraper service temporarily unavailable");
            }

            log.debug("Scraping content for request {} from URL: {}", requestId, url);

            // Perform scraping
            ScrapeResult result = scrapeTool.scrapeWebpage(url);

            // Null check
            if (result == null) {
                log.warn("Scraper returned null result for request {}", requestId);
                return createErrorResult(url, "Scraping failed - no result");
            }

            log.debug("Scraping completed for request {} - success: {}", requestId, result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("Scraping failed for request {} on URL {}: {}", requestId, url, e.getMessage());
            return createErrorResult(url, "Scraping operation failed: " + e.getMessage());
        } finally {
            // Always return the instance
            if (scrapeTool != null) {
                playwrightAllocator.returnScraperInstance(scrapeTool, requestId);
            }
        }
    }

    @Cacheable(value = "scrapedPages", key = "#url")
    public ScrapeResult scrapeContent_withoutLock(int requestId, String url) {
        if (url == null || url.trim().isEmpty()) {
            log.warn("Empty URL provided for scraping request {}", requestId);
            return createErrorResult(url, "Empty URL provided");
        }

        try {
            PlaywrightWebScraperTools scraperInstance = playwrightAllocatorWithoutLock.getScraperInstance_withoutLock(requestId);

            if (scraperInstance == null) {
                log.error("No scraper instance available for request {}", requestId);
                return createErrorResult(url, "Scraper service temporarily unavailable");
            }

            log.debug("Scraping content for request {} from URL: {}", requestId, url);
            ScrapeResult result = scraperInstance.scrapeWebpage(url);

            if (result == null) {
                log.warn("Scraper returned null result for request {}", requestId);
                return createErrorResult(url, "Scraping failed - no result");
            }

            log.debug("Scraping completed for request {} - success: {}", requestId, result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("Scraping failed for request {} on URL {}: {}", requestId, url, e.getMessage());
            return createErrorResult(url, "Scraping operation failed");
        }
    }

    // Error object response
    private ScrapeResult createErrorResult(String url, String errorMessage) {
        ScrapeResult errorResult = new ScrapeResult();
        errorResult.setSuccess(false);
        errorResult.setContent(null);
        errorResult.setUrl(url);
        errorResult.setError(errorMessage);
        return errorResult;
    }

    @Profile("prod")
    public boolean isServiceHealthy() {
        return playwrightAllocator != null && playwrightAllocator.isInitialized();
    }

    @Profile("dev")
    public boolean isServiceHealthy_dev() {
        return playwrightAllocatorWithoutLock != null && playwrightAllocatorWithoutLock.isInitialized();
    }
}

package com.mcp.webScraper.Services;

import com.mcp.webScraper.Entries.SearchResult;
import com.mcp.webScraper.Workers.PlaywrightAllocator;
import com.mcp.webScraper.Workers.PlaywrightAllocator_withoutLock;
import com.mcp.webScraper.Workers.PlaywrightBrowserSearchTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServices {

    private static final Logger log = LoggerFactory.getLogger(SearchServices.class);

    @Autowired(required = false)
    private PlaywrightAllocator playwrightAllocator;

    @Autowired(required = false)
    private PlaywrightAllocator_withoutLock playwrightAllocatorWithoutLock;

    @Cacheable(value = "searchResults", key = "#query + '_' + #maxResults")
    public List<SearchResult> performSearch(int requestId, String query, int maxResults) {
        // Input validation
        if (query == null || query.trim().isEmpty()) {
            log.warn("Empty query provided for request {}", requestId);
            return new ArrayList<>();
        }

        PlaywrightBrowserSearchTools searchTool = null;

        try {
            // Borrow instance
            searchTool = playwrightAllocator.borrowSearchInstance(requestId);
            if (searchTool == null) {
                log.error("No search instance available for request {}", requestId);
                return createErrorResult("Search service temporarily unavailable");
            }

            log.debug("Performing search for request {} with query: '{}'", requestId, query);

            // Perform search
            List<SearchResult> results = searchTool.playwrightSearch(query, maxResults, "duckduckgo");

            // Null check
            if (results == null) {
                log.warn("Search returned null results for request {}", requestId);
                return new ArrayList<>();
            }

            log.info("Search completed for request {} - {} results found", requestId, results.size());
            return results;

        } catch (Exception e) {
            log.error("Search failed for request {} with query '{}': {}", requestId, query, e.getMessage());
            return createErrorResult("Search operation failed: " + e.getMessage());
        } finally {
            // Always return the instance
            if (searchTool != null) {
                playwrightAllocator.returnSearchInstance(searchTool, requestId);
            }
        }
    }

    @Cacheable(value = "searchResults", key = "#query + '_' + #maxResults")
    public List<SearchResult> performSearch_withoutLock(int requestId, String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Empty query provided for request {}", requestId);
            return new ArrayList<>();
        }

        try {
            PlaywrightBrowserSearchTools searchInstance = playwrightAllocatorWithoutLock.getSearchInstance_withoutLock(requestId);

            if (searchInstance == null) {
                log.error("No search instance available for request {}", requestId);
                return createErrorResult("Search service temporarily unavailable");
            }

            log.debug("Performing search for request {} with query: '{}'", requestId, query);
            List<SearchResult> results = searchInstance.playwrightSearch(query, maxResults, "duckduckgo");

            if (results == null) {
                log.warn("Search returned null results for request {}", requestId);
                return new ArrayList<>();
            }

            log.info("Search completed for request {} - {} results found", requestId, results.size());
            return results;

        } catch (Exception e) {
            log.error("Search failed for request {}: {}", requestId, e.getMessage());
            return createErrorResult("Search operation failed");
        }
    }

    // Error object response
    private List<SearchResult> createErrorResult(String errorMessage) {
        SearchResult errorResult = new SearchResult();
        errorResult.setSuccess(false);
        errorResult.setSource("error");
        errorResult.setContent(errorMessage);
        errorResult.setError(errorMessage);

        List<SearchResult> errorList = new ArrayList<>();
        errorList.add(errorResult);
        return errorList;
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

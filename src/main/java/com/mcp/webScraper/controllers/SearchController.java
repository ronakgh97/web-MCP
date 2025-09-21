package com.mcp.webScraper.controllers;

import com.mcp.webScraper.dtos.Query;
import com.mcp.webScraper.entries.RequestEntries;
import com.mcp.webScraper.entries.ResponseEntries;
import com.mcp.webScraper.entries.SearchResult;
import com.mcp.webScraper.workers.PlaywrightBrowserSearchTools;
import com.mcp.webScraper.workers.PlaywrightWebScraperTools;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This controller handles the web scraping requests.
 * It uses Playwright to perform searches and scrape web pages.
 */
@RestController
@RequestMapping("api/v1/search")
public class SearchController {

    // Number of Playwright instances for searching and scraping.
    // This allows for parallel processing of requests.
    private final int playwrightSearchInstances = 10;
    private final int playwrightScrapeInstances = 10;

    // Arrays to hold the Playwright instances.
    private final PlaywrightBrowserSearchTools[] playwrightBrowserSearchToolsArr = new PlaywrightBrowserSearchTools[playwrightSearchInstances];

    private final PlaywrightWebScraperTools[] playwrightWebScraperToolsArr = new PlaywrightWebScraperTools[playwrightScrapeInstances];

    /**
     * Constructor for the SearchController.
     * Initializes the Playwright instances.
     */
    public SearchController() {
        for (int i = 0; i < playwrightSearchInstances; i++) {
            playwrightBrowserSearchToolsArr[i] = new PlaywrightBrowserSearchTools();
        }
        for (int i = 0; i < playwrightScrapeInstances; i++) {
            playwrightWebScraperToolsArr[i] = new PlaywrightWebScraperTools();
        }
    }

    @PostMapping
    public ResponseEntity<ResponseEntries> search(@Valid @RequestBody Query userQuery) {
        RequestEntries requestEntries = new RequestEntries();
        ResponseEntries responseEntries = new ResponseEntries();
        requestEntries.setQuery(userQuery.getQuery());
        int req_ID = responseEntries.getResponseId();

        // Use a simple hash routing to distribute requests among the Playwright instances.
        // This helps to balance the load and avoid conflict over a single instance safely.
        int routerSearch = req_ID % playwrightSearchInstances;
        int routerScrape = req_ID % playwrightScrapeInstances;
        responseEntries.setMessage("Routing to #" + routerSearch + ", #" + routerScrape + " instances");

        responseEntries.setUserQuery(requestEntries.getQuery());

        // Use the routed browser/search instance to perform the search.
        List<SearchResult> searchResults = playwrightBrowserSearchToolsArr[routerSearch]
                .playwrightSearch(requestEntries.getQuery(), "duckduckgo");

        // Scrape the content of each search result using the routed scraper instance.
        for (SearchResult result : searchResults) {
            result.setContent(playwrightWebScraperToolsArr[routerScrape]
                    .scrapeWebpage(result.getSource())
                    .getContent());
        }

        responseEntries.setSearchResultList(searchResults);

        // Return an error if no search results are found.
        if (searchResults.isEmpty()) {
            return new ResponseEntity<>(responseEntries, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(responseEntries, HttpStatus.OK);
    }

}

package com.mcp.webScraper.Workers;

import com.mcp.webScraper.Entries.SearchResult;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Geolocation;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.mcp.webScraper.Workers.PlaywrightConfig.*;

/**
 * This service provides the functionality to search on the web using Playwright.
 * It supports multiple search engines and can fall back to changes in the search engine's website.
 */
@Service
public class PlaywrightBrowserSearchTools {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserSearchTools.class);

    // The maximum number of search results to return per search engine.
    private static final int MAX_RESULTS_PER_ENGINE = 3;

    // A map of search engines that are supported by this service.
    // Each search engine has a name, a URL template, and selectors for the search results, titles, and snippets.
    private static final Map<String, SearchEngine> ENGINES = Map.of(
            "duckduckgo", new SearchEngine(
                    "DuckDuckGo",
                    "https://duckduckgo.com/?q=%s&t=h_&ia=web",
                    "[data-testid='result'], .react-results--main .result, #links .result, article[data-testid='result']",
                    "[data-testid='result-title-a'], h2 a, h3 a, .result__title a",
                    "[data-testid='result-snippet'], .result__snippet, .snippet, p"
            )
    );

    // Instance variables for Playwright and the browser.
    private volatile Playwright playwright;
    private volatile Browser browser;
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong searchCount = new AtomicLong(0);
    private AtomicBoolean isUse = new AtomicBoolean(false);

    /**
     * Constructor for the PlaywrightBrowserSearchTools.
     * Initializes the Playwright browser.
     */
    public PlaywrightBrowserSearchTools() {
        //logger.info("Initializing Playwright search tool...");
        initializeBrowser();
    }

    /**
     * This method is called before the bean is destroyed.
     * It closes the Playwright browser and releases any resources.
     */
    public void cleanup() {
        //logger.info("Shutting down Playwright search tool");
        try {
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    public boolean isInUse() {
        return this.isUse.get();
    }

    public void setInUse(boolean inUse) {
        this.isUse.set(inUse);
    }

    /**
     * Attempts to acquire the lock.
     *
     * @return {@code true} if the lock was acquired, {@code false} otherwise.
     */
    public boolean tryAcquire() {
        return isUse.compareAndSet(false, true);
    }

    /**
     * Releases the lock.
     */
    public void release() {
        isUse.set(false);
    }

    /**
     * This is the main method for performing a web search.
     * It takes a query and a search engine, and returns a list of search results.
     */
    public List<SearchResult> playwrightSearch(String query, int maxResults, String engine) {
        if (browser == null) {
            logger.warn("Browser not initialized");
            return sendError("Browser is not initialized!!");
        }

        long searchId = searchCount.incrementAndGet();
        logger.debug("Starting search #{}: '{}'", searchId, query);

        // Validate the input query.
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty query for search #{}", searchId);
            return sendError("Query is empty!!");
        }

        try (BrowserContext context = createContext()) {
            Page page = context.newPage();
            setupPage(page);

            List<SearchResult> results = performSearch(page, query.trim(), engine, searchId, maxResults);
            logger.info("Search #{} completed with {} results", searchId, results.size());
            return results;

        } catch (Exception e) {
            logger.error("Search #{} failed: {}", searchId, e.getMessage());
            return sendError("Search failed!!");
        }
    }

    /**
     * This method performs the actual search on the search engine's website.
     * It iterates through the configured search engines and tries to find results.
     */
    private List<SearchResult> performSearch(Page page, String query, String preferredEngine, long searchId, int maxResults) {
        List<String> engineOrder = determineEngineOrder(preferredEngine);

        for (String engineKey : engineOrder) {
            SearchEngine engine = ENGINES.get(engineKey);
            if (engine == null) continue;

            try {
                logger.debug("Trying {} for search #{}", engine.name, searchId);

                // Navigate to the search engine's website.
                String searchUrl = String.format(engine.urlTemplate, URLEncoder.encode(query, StandardCharsets.UTF_8));
                page.navigate(searchUrl, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(NAVIGATION_TIMEOUT_MS));

                // Wait for the search results to appear on the page.
                if (!tryMultipleSelectors(page, engine.resultSelector.split(", "))) {
                    logger.debug("No results found with any selector for search #{}", searchId);
                    continue; // Try next engine
                }

                // Add a human-like delay to avoid being detected as a bot.
                page.waitForTimeout(WAIT_TIMEOUT_MS);

                // Extract the search results from the page.
                List<SearchResult> results = extractResults(page, engine, searchId, maxResults);

                logger.debug("Engine {} returned {} SearchResult objects for search #{}",
                        engine.name, results.size(), searchId);

                if (!results.isEmpty()) {
                    logger.info("Search #{} successful with {} - {} results", searchId, engine.name, results.size());
                    return results;
                } else {
                    logger.debug("Engine {} returned empty results", engine.name);
                }

            } catch (Exception e) {
                logger.debug("Search #{} failed with {}: {}", searchId, engine.name, e.getMessage());
            }
        }

        return sendError("Something went Wrong");
    }

    /**
     * This method extracts the search results from the page.
     * It uses the configured selectors to find the title, link, and snippet of each search result.
     */
    private List<SearchResult> extractResults(Page page, SearchEngine engine, long searchId, int maxResults) {
        List<SearchResult> results = new ArrayList<>();

        try {
            Locator resultElements = page.locator(engine.resultSelector);
            int count = Math.min(resultElements.count(), maxResults);

            logger.debug("Found {} results for search #{}", count, searchId);

            for (int i = 0; i < count; i++) {
                try {
                    Locator result = resultElements.nth(i);

                    // Try different selectors for the title and link to make the scraping more robust.
                    String title = null;
                    String link = null;

                    for (String titleSel : engine.titleSelector.split(", ")) {
                        title = getTextSafely(result.locator(titleSel.trim()));
                        link = getLinkSafely(result.locator(titleSel.trim()));
                        if (title != null && link != null) break;
                    }

                    String snippet = getTextSafely(result.locator(engine.snippetSelector));

                    if (title != null && link != null && !link.trim().isEmpty()) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setSuccess(true);
                        searchResult.setSource(link.trim());
                        searchResult.setSnippet(snippet != null ? snippet.trim() : title.trim());
                        searchResult.setError(null);
                        results.add(searchResult);
                    }

                } catch (Exception e) {
                    logger.debug("Failed to extract result #{}", i + 1);
                }
            }

        } catch (Exception e) {
            logger.error("Result extraction failed for search #{}: {}", searchId, e.getMessage());
            return sendError("Failed to extract result or empty field");
        }

        return results;
    }

    /**
     * This method tries to find an element on the page using multiple selectors.
     * This is useful when a website has different layouts or class names for the same element.
     */
    private boolean tryMultipleSelectors(Page page, String[] selectors) {
        for (String selector : selectors) {
            try {
                page.waitForSelector(selector.trim(), new Page.WaitForSelectorOptions()
                        .setTimeout(PlaywrightConfig.SELECTOR_WAIT_TIMEOUT_MS)
                        .setState(WaitForSelectorState.ATTACHED));

                // Verify that the element is actually visible.
                Locator elements = page.locator(selector.trim());
                if (elements.first().isVisible()) {
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Selector '{}' failed: {}", selector.trim(), e.getMessage());
            }
        }
        return false;
    }

    /**
     * This method safely extracts the text content of an element.
     */
    private String getTextSafely(Locator locator) {
        try {
            return locator.count() > 0 ? locator.textContent() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * This method safely extracts the href attribute of a link.
     */
    private String getLinkSafely(Locator locator) {
        try {
            if (locator.count() > 0) {
                String href = locator.getAttribute("href");
                return href != null && href.startsWith("http") ? href : null;
            }
        } catch (Exception e) {
            // Ignore any exceptions.
        }
        return null;
    }

    /**
     * This method determines the order of search engines to use.
     * It prioritizes the preferred engine, and then adds the remaining engines.
     */
    private List<String> determineEngineOrder(String preferred) {
        List<String> order = new ArrayList<>();

        if (preferred != null && ENGINES.containsKey(preferred.toLowerCase())) {
            order.add(preferred.toLowerCase());
        }

        // Add the remaining engines.
        for (String engine : Arrays.asList("duckduckgo")) {
            if (!order.contains(engine)) {
                order.add(engine);
            }
        }

        return order;
    }

    /**
     * This method initializes the Playwright browser.
     * It creates a new Playwright instance and launches a Chromium browser.
     */
    private void initializeBrowser() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(BROWSER_HEADLESS)
                    .setTimeout(DEFAULT_TIMEOUT_MS)
                    .setArgs(BROWSER_ARGS));
            //logger.info("Browser initialized successfully");
        } catch (Exception e) {
            logger.error("Browser initialization failed", e.getMessage());
        }
    }

    /**
     * This method creates a new browser context.
     * It sets a random user agent and other browser options to avoid being detected as a bot.
     */
    private BrowserContext createContext() {
        LocationProfile profile = RESIDENTIAL_PROFILES.get(random.nextInt(RESIDENTIAL_PROFILES.size()));

        double latVariation = (random.nextDouble() - 0.5) * 0.02; // ~1km variation
        double lonVariation = (random.nextDouble() - 0.5) * 0.02;

        return browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(getRandomUserAgent())
                .setGeolocation(new Geolocation(
                        profile.getLat() + latVariation,
                        profile.getLon() + lonVariation))
                .setPermissions(Arrays.asList("geolocation"))
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setTimezoneId(profile.getTimezone())
                .setLocale(profile.getLocale())
                .setBypassCSP(true)
                .setIgnoreHTTPSErrors(true)
                .setJavaScriptEnabled(true)
                .setExtraHTTPHeaders(DEFAULT_HEADERS));
    }

    /**
     * This method sets up the page for scraping.
     * It adds a stealth script to avoid bot detection and blocks unnecessary resources to speed up the scraping.
     */
    private void setupPage(Page page) {
        page.addInitScript(STEALTH_SCRIPT);

        page.route("**/*", route -> {
            String url = route.request().url();
            String resourceType = route.request().resourceType();

            try {
                // Block unnecessary resources such as images, stylesheets, fonts, and media.
                if (resourceType.equals("image") || resourceType.equals("stylesheet") ||
                        resourceType.equals("font") || resourceType.equals("media") ||
                        url.contains("ads")) {
                    route.abort();
                } else {
                    route.resume();
                }
            } catch (Exception e) {
                // If routing fails, try to resume the request.
                try {
                    route.resume();
                } catch (Exception ignored) {
                    logger.debug("Route handling failed for: {}", url);
                }
            }
        });
    }


    /**
     * This method returns a random user agent from a list of user agents.
     */
    private String getRandomUserAgent() {
        String user = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        logger.debug("User_Agent: {}", user);
        return user;
    }

    /**
     * This method returns null objects with error message.
     */
    private List<SearchResult> sendError(String errorMessage) {
        return List.of(new SearchResult(false, null, null, null, errorMessage));
    }

    /**
     * This class represents a search engine configuration.
     * It contains the name, URL template, and selectors for the search engine.
     */
    private static class SearchEngine {
        final String name;
        final String urlTemplate;
        final String resultSelector;
        final String titleSelector;
        final String snippetSelector;

        SearchEngine(String name, String urlTemplate, String resultSelector,
                     String titleSelector, String snippetSelector) {
            this.name = name;
            this.urlTemplate = urlTemplate;
            this.resultSelector = resultSelector;
            this.titleSelector = titleSelector;
            this.snippetSelector = snippetSelector;
        }
    }
}
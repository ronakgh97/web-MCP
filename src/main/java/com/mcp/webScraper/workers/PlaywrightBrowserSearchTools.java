package com.mcp.webScraper.workers;

import com.mcp.webScraper.entries.SearchResult;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.mcp.webScraper.workers.PlaywrightConfig.*;

@Service
public class PlaywrightBrowserSearchTools {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserSearchTools.class);

    // Search Configuration
    private static final int MAX_DUCKDUCKGO_RESULTS = 1;
    private static final int MAX_BING_RESULTS = 1;
    private static final int DUCKDUCKGO_SNIPPET_LENGTH = 1500;
    private static final int BING_SNIPPET_LENGTH = 1255;
    private static final int URL_DISPLAY_MAX_LENGTH = 100;
    private static final int URL_SUFFIX_LENGTH = 100;
    private static final int TRUNCATE_THRESHOLD = 1200;

    // Search Engine URLs
    private static final String DUCKDUCKGO_URL_TEMPLATE = "https://duckduckgo.com/?q=%s&t=h_&ia=web";
    private static final String BING_URL_TEMPLATE = "https://www.bing.com/search?q=%s&form=QBLH";

    // Search Engine Configuration Map
    private static final Map<String, SearchEngineConfig> SEARCH_ENGINES = Map.of(
            "duckduckgo", new SearchEngineConfig(
                    "DuckDuckGo",
                    DUCKDUCKGO_URL_TEMPLATE,
                    new String[]{
                            "[data-testid='result']",
                            ".react-results--main .result",
                            "#links .result",
                            "article[data-testid='result']",
                            ".web-result"
                    },
                    "[data-testid='result-title-a']",
                    "h2 a, h3 a, .result__title a",
                    "[data-testid='result-snippet']",
                    ".result__snippet, .snippet, p",
                    MAX_DUCKDUCKGO_RESULTS,
                    DUCKDUCKGO_SNIPPET_LENGTH,
                    "🦆"
            ),
            "bing", new SearchEngineConfig(
                    "Bing",
                    BING_URL_TEMPLATE,
                    new String[]{
                            ".b_algo",
                            "#b_results .b_algo",
                            ".b_searchResult",
                            "[data-bm]"
                    },
                    "h2 a",
                    ".b_title a, h2 a, h3 a",
                    ".b_caption p",
                    ".b_caption, .b_snippet, .b_dList",
                    MAX_BING_RESULTS,
                    BING_SNIPPET_LENGTH,
                    "🔍"
            )
    );

    // Default search engine order
    private static final List<String> DEFAULT_SEARCH_ORDER = List.of("duckduckgo", "bing");

    // Resource blocking patterns
    private static final String BLOCKED_RESOURCES = "**/*.{png,jpg,jpeg,gif,svg,woff,woff2}";

    // Instance variables
    private volatile Playwright playwright;
    private volatile Browser browser;
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong searchCount = new AtomicLong(0);

    public PlaywrightBrowserSearchTools() {
        logger.info("Initializing Playwright browser search tool...");
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(BROWSER_HEADLESS)
                    .setTimeout(DEFAULT_TIMEOUT_MS)
                    .setArgs(BROWSER_ARGS)
            );
            logger.info("Playwright browser initialized successfully. Available engines: {}",
                    String.join(", ", SEARCH_ENGINES.keySet()));
        } catch (Exception e) {
            logger.error("Asynchronous Playwright browser initialization failed.", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down Playwright browser search tool");
        try {
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            logger.info("Playwright resources cleaned up successfully. Total searches: {}", searchCount.get());
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

    public List<SearchResult> playwrightSearch(
            String query,
            String engine) {

        if (browser == null) {
            logger.warn("Playwright browser is not yet initialized. Please try again in a moment.");
            return new ArrayList<>();
        }

        long searchId = searchCount.incrementAndGet();
        logger.debug("Starting browser search #{} for query: '{}' with engine preference: '{}'",
                searchId, query, engine);

        // Validate input
        SearchRequest request = validateSearchRequest(query, engine);
        if (!request.isValid()) {
            logger.warn("Invalid search request #{}: {}", searchId, request.getErrorMessage());
            return new ArrayList<>();
        }

        BrowserContext context = null;
        try {
            String userAgent = getRandomUserAgent();

            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(userAgent)
                    .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                    .setJavaScriptEnabled(true)
                    .setExtraHTTPHeaders(createHeaders())
            );

            Page page = context.newPage();
            configurePageStealth(page);
            setupResourceBlocking(page);

            List<SearchResult> result = searchWithMultipleEngines(page, request, searchId);

            logger.info("Browser search #{} completed successfully", searchId);
            return result;

        } catch (Exception e) {
            logger.error("Browser search #{} failed: {}", searchId, e.getMessage());
            return new ArrayList<>();
        } finally {
            if (context != null) context.close();
        }
    }

    private SearchRequest validateSearchRequest(String query, String engine) {
        if (query == null || query.trim().isEmpty()) {
            return SearchRequest.invalid("Search query cannot be empty.");
        }

        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > 200) {
            return SearchRequest.invalid("Search query is too long (maximum 200 characters).");
        }

        String preferredEngine = null;
        if (engine != null && !engine.trim().isEmpty()) {
            String normalizedEngine = engine.trim().toLowerCase();
            if (!SEARCH_ENGINES.containsKey(normalizedEngine)) {
                return SearchRequest.invalid("Invalid search engine. Available options: " +
                        String.join(", ", SEARCH_ENGINES.keySet()));
            }
            preferredEngine = normalizedEngine;
        }

        return SearchRequest.valid(trimmedQuery, preferredEngine);
    }

    private List<SearchResult> searchWithMultipleEngines(Page page, SearchRequest request, long searchId) {
        List<String> engineOrder = determineSearchOrder(request.getPreferredEngine());
        List<SearchResult> searchResultList = new ArrayList<>();

        for (String engineKey : engineOrder) {
            SearchEngineConfig config = SEARCH_ENGINES.get(engineKey);
            if (config == null) continue;

            try {
                logger.debug("Attempting search #{} with {} engine", searchId, config.name);
                SearchEngineResult result = performSearchWithEngine(page, request.getQuery(), config, searchId);

                if (result.hasResults()) {
                    for (SearchResultData item : result.items) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setSource(item.link);
                        searchResult.setContent(null);
                        searchResultList.add(searchResult);
                    }

                    logger.info("Search #{} successful with {} engine - {} results",
                            searchId, config.name, result.getResultCount());

                    // If we have results from preferred engine or DuckDuckGo, we can stop
                    if (request.getPreferredEngine() != null || "duckduckgo".equals(engineKey)) {
                        break;
                    }
                } else {
                    logger.debug("Search #{} with {} engine returned no results", searchId, config.name);
                }

            } catch (Exception e) {
                logger.warn("Search #{} error with {} engine: {}", searchId, config.name, e.getMessage());
                continue;
            }
        }

        return !searchResultList.isEmpty() ? searchResultList : new ArrayList<>();
    }

    private List<String> determineSearchOrder(String preferredEngine) {
        List<String> order = new ArrayList<>();

        // Add preferred engine first if specified
        if (preferredEngine != null) {
            order.add(preferredEngine);
        }

        // Add remaining engines from default order
        for (String engine : DEFAULT_SEARCH_ORDER) {
            if (!order.contains(engine)) {
                order.add(engine);
            }
        }

        return order;
    }

    private SearchEngineResult performSearchWithEngine(Page page, String query, SearchEngineConfig config, long searchId)
            throws SearchException {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = String.format(config.urlTemplate, encodedQuery);

            logger.debug("Navigating to {} for search #{}", config.name, searchId);
            page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // Random wait time (different for different engines)
            int waitTime = BASE_WAIT_TIME_MS + random.nextInt(RANDOM_WAIT_TIME_MS);
            if ("bing".equals(config.name.toLowerCase())) {
                waitTime += 500; // Bing needs a bit more time
            }
            page.waitForTimeout(waitTime);

            List<ElementHandle> results = findSearchResults(page, config);
            if (results.isEmpty()) {
                return SearchEngineResult.empty(config.name, query);
            }

            List<SearchResultData> resultData = extractSearchResults(results, config);
            return new SearchEngineResult(config.name, query, resultData, config);

        } catch (TimeoutError e) {
            throw new SearchException(config.name + " search timed out");
        } catch (Exception e) {
            throw new SearchException(config.name + " search failed: " + e.getMessage());
        }
    }

    private List<ElementHandle> findSearchResults(Page page, SearchEngineConfig config) {
        List<ElementHandle> results = null;

        for (String selector : config.resultSelectors) {
            try {
                page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                        .setTimeout(SELECTOR_TIMEOUT_MS)
                        .setState(WaitForSelectorState.VISIBLE));
                results = page.querySelectorAll(selector);
                if (!results.isEmpty()) {
                    logger.debug("Found {} results using {} selector: {}",
                            results.size(), config.name, selector);
                    break;
                }
            } catch (TimeoutError e) {
                logger.debug("{} selector {} timed out, trying next", config.name, selector);
                continue;
            }
        }

        return results != null ? results : Collections.emptyList();
    }

    private List<SearchResultData> extractSearchResults(List<ElementHandle> results, SearchEngineConfig config) {
        List<SearchResultData> extractedResults = new ArrayList<>();

        for (int i = 0; i < Math.min(results.size(), config.maxResults); i++) {
            try {
                ElementHandle result = results.get(i);
                SearchResultData resultData = extractResultData(result, config);
                if (resultData.isValid()) {
                    extractedResults.add(resultData);
                }
            } catch (Exception e) {
                logger.debug("Failed to extract result {}: {}", i + 1, e.getMessage());
                continue;
            }
        }

        return extractedResults;
    }

    private SearchResultData extractResultData(ElementHandle result, SearchEngineConfig config) {
        // Try primary title selector first, then fallback
        ElementHandle titleElement = result.querySelector(config.primaryTitleSelector);
        if (titleElement == null) {
            titleElement = result.querySelector(config.fallbackTitleSelectors);
        }

        // Try primary snippet selector first, then fallback
        ElementHandle snippetElement = result.querySelector(config.primarySnippetSelector);
        if (snippetElement == null) {
            snippetElement = result.querySelector(config.fallbackSnippetSelectors);
        }

        if (titleElement != null && snippetElement != null) {
            String title = titleElement.textContent();
            String link = titleElement.getAttribute("href");
            String snippet = snippetElement.textContent();

            return new SearchResultData(title, link, snippet);
        }

        return SearchResultData.invalid();
    }

    private String getRandomUserAgent() {
        String selectedAgent = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        logger.debug("Selected user agent: {}...", selectedAgent.substring(0, Math.min(50, selectedAgent.length())));
        return selectedAgent;
    }

    private Map<String, String> createHeaders() {
        String acceptLanguage = ACCEPT_LANGUAGES.get(random.nextInt(ACCEPT_LANGUAGES.size()));
        return DEFAULT_HEADERS;
    }

    private void configurePageStealth(Page page) {
        String stealthScript = String.format(STEALTH_SCRIPT);
        page.addInitScript(stealthScript);
    }

    private void setupResourceBlocking(Page page) {
        page.route(BLOCKED_RESOURCES, route -> route.abort());
    }

    private static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;

        int lastSpace = text.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength - TRUNCATE_THRESHOLD) {
            return text.substring(0, lastSpace) + "...";
        }
        return text.substring(0, maxLength) + "...";
    }

    private static String shortenUrl(String url) {
        if (url == null || url.length() <= URL_DISPLAY_MAX_LENGTH) return url;

        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost() + "..." + url.substring(url.length() - URL_SUFFIX_LENGTH);
        } catch (Exception e) {
            return url.substring(0, URL_DISPLAY_MAX_LENGTH) + "...";
        }
    }

    // Helper classes
    private static class SearchEngineConfig {
        final String name;
        final String urlTemplate;
        final String[] resultSelectors;
        final String primaryTitleSelector;
        final String fallbackTitleSelectors;
        final String primarySnippetSelector;
        final String fallbackSnippetSelectors;
        final int maxResults;
        final int snippetLength;
        final String emoji;

        SearchEngineConfig(String name, String urlTemplate, String[] resultSelectors,
                           String primaryTitleSelector, String fallbackTitleSelectors,
                           String primarySnippetSelector, String fallbackSnippetSelectors,
                           int maxResults, int snippetLength, String emoji) {
            this.name = name;
            this.urlTemplate = urlTemplate;
            this.resultSelectors = resultSelectors;
            this.primaryTitleSelector = primaryTitleSelector;
            this.fallbackTitleSelectors = fallbackTitleSelectors;
            this.primarySnippetSelector = primarySnippetSelector;
            this.fallbackSnippetSelectors = fallbackSnippetSelectors;
            this.maxResults = maxResults;
            this.snippetLength = snippetLength;
            this.emoji = emoji;
        }
    }

    private static class SearchRequest {
        private final boolean valid;
        private final String errorMessage;
        private final String query;
        private final String preferredEngine;

        private SearchRequest(boolean valid, String errorMessage, String query, String preferredEngine) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.query = query;
            this.preferredEngine = preferredEngine;
        }

        static SearchRequest valid(String query, String preferredEngine) {
            return new SearchRequest(true, null, query, preferredEngine);
        }

        static SearchRequest invalid(String errorMessage) {
            return new SearchRequest(false, errorMessage, null, null);
        }

        boolean isValid() {
            return valid;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        String getQuery() {
            return query;
        }

        String getPreferredEngine() {
            return preferredEngine;
        }
    }

    private static class SearchResultData {
        final String title;
        final String link;
        final String snippet;
        final boolean valid;

        SearchResultData(String title, String link, String snippet) {
            this.title = title;
            this.link = link;
            this.snippet = snippet;
            this.valid = title != null && !title.isEmpty() && link != null && !link.isEmpty();
        }

        static SearchResultData invalid() {
            return new SearchResultData(null, null, null);
        }

        boolean isValid() {
            return valid;
        }
    }

    private static class SearchEngineResult {
        private final String engineName;
        private final String query;
        private final List<SearchResultData> items;
        private final SearchEngineConfig config;

        SearchEngineResult(String engineName, String query, List<SearchResultData> items, SearchEngineConfig config) {
            this.engineName = engineName;
            this.query = query;
            this.items = items != null ? items : Collections.emptyList();
            this.config = config;
        }

        static SearchEngineResult empty(String engineName, String query) {
            return new SearchEngineResult(engineName, query, Collections.emptyList(), null);
        }

        boolean hasResults() {
            return !items.isEmpty();
        }

        int getResultCount() {
            return items.size();
        }

        String getFormattedResults() {
            if (items.isEmpty()) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            result.append(config.emoji).append(" **").append(engineName).append(" Results for: ")
                    .append(query).append("** ");

            for (int i = 0; i < items.size(); i++) {
                SearchResultData item = items.get(i);
                result.append("**").append(i + 1).append(". ").append(item.title).append("** ");

                if (!item.snippet.isEmpty()) {
                    result.append("📝 ").append(truncateText(item.snippet, config.snippetLength)).append(" ");
                }

                result.append("🔗 ").append(shortenUrl(item.link)).append(" ");
            }

            return result.toString();
        }
    }

    // Custom exception
    private static class SearchException extends Exception {
        SearchException(String message) {
            super(message);
        }
    }
}
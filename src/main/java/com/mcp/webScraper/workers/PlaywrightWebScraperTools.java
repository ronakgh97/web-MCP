package com.mcp.webScraper.workers;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import static com.mcp.webScraper.workers.PlaywrightConfig.*;

@Service
public class PlaywrightWebScraperTools {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightWebScraperTools.class);

    // CSS Selectors
    private static final String MAIN_CONTENT_SELECTORS = "main, article, .content, .post, .entry";
    private static final String REMOVE_ELEMENTS_SELECTORS = "script, style, nav, header, footer, aside";
    private static final String META_DESCRIPTION_SELECTOR = "meta[name=description]";
    private static final String AUTHOR_SELECTORS = "meta[name=author], .author, .byline";
    private static final String DATE_SELECTORS = "time, .date, .published, meta[property='article:published_time']";

    // Structured data selectors
    private static final String TABLE_SELECTOR = "table";
    private static final String LIST_SELECTOR = "ul, ol";
    private static final String LINK_SELECTOR = "a[href]";
    private static final String IMAGE_SELECTOR = "img[src]";

    // Instance variables
    private volatile Playwright playwright;
    private volatile Browser browser;
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong scrapeCount = new AtomicLong(0);

    public PlaywrightWebScraperTools() {
        logger.info("Initializing Playwright web scraper tool...");
        try {
            playwright = Playwright.create();
            browser = createBrowser();
            logger.info("Playwright web scraper initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Playwright web scraper", e);
            throw new ScraperInitializationException("Failed to initialize web scraper", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down Playwright web scraper tool");
        try {
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            logger.info("Playwright web scraper cleaned up successfully. Total scrapes: {}", scrapeCount.get());
        } catch (Exception e) {
            logger.error("Error during web scraper cleanup", e);
        }
    }

    public String scrape_webpage(String url) {
        long scrapeId = scrapeCount.incrementAndGet();
        logger.debug("Starting webpage scrape #{} for URL: {}", scrapeId, url);

        // Validate URL
        UrlValidationResult validation = validateUrl(url);
        if (!validation.isValid()) {
            logger.warn("Invalid URL for scrape #{}: {}", scrapeId, validation.getErrorMessage());
            return "❌ " + validation.getErrorMessage();
        }

        try {
            String html = fetchPageContent(url, scrapeId);
            String result = parseWebpageContent(html, url);

            logger.info("Webpage scrape #{} completed successfully for domain: {}",
                    scrapeId, extractDomain(url));
            return result;

        } catch (ScrapingException e) {
            logger.error("Webpage scrape #{} failed: {}", scrapeId, e.getMessage());
            return "❌ " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error during webpage scrape #{}", scrapeId, e);
            return "❌ An unexpected error occurred while scraping the webpage.";
        }
    }

    private Browser createBrowser() throws ScraperInitializationException {
        try {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(BROWSER_HEADLESS)
                    .setArgs(BROWSER_ARGS);

            return playwright.chromium().launch(launchOptions);
        } catch (Exception e) {
            throw new ScraperInitializationException("Failed to create browser", e);
        }
    }

    private String fetchPageContent(String url, long scrapeId) throws ScrapingException {
        BrowserContext context = null;
        try {
            String userAgent = getRandomUserAgent();

            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(userAgent)
                    .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                    .setJavaScriptEnabled(true)
                    .setExtraHTTPHeaders(DEFAULT_HEADERS)
            );

            Page page = context.newPage();
            configurePageStealth(page);
            setPageTimeouts(page);

            return fetchWithRetry(page, url, scrapeId);

        } catch (Exception e) {
            throw new ScrapingException("Failed to fetch page content: " + e.getMessage(), e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    logger.warn("Error closing browser context for scrape #{}", scrapeId, e);
                }
            }
        }
    }

    private String fetchWithRetry(Page page, String url, long scrapeId) throws ScrapingException {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("Attempt {} of {} for scrape #{}", attempt, MAX_RETRY_ATTEMPTS, scrapeId);

                Response response = page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(NAVIGATION_TIMEOUT_MS));

                if (response == null || !response.ok()) {
                    throw new ScrapingException("Failed to load page: " +
                            (response != null ? "HTTP " + response.status() : "No response"));
                }

                // Wait for network to be idle
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_TIMEOUT_MS));

                // Additional wait for dynamic content
                page.waitForTimeout(ADDITIONAL_WAIT_MS);

                return page.content();

            } catch (Exception e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new ScrapingException("Failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage(), e);
                }

                logger.warn("Attempt {} failed for scrape #{}, retrying: {}", attempt, scrapeId, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_BASE_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ScrapingException("Scraping interrupted", ie);
                }
            }
        }

        throw new ScrapingException("Unexpected end of retry loop");
    }

    private UrlValidationResult validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return UrlValidationResult.invalid("URL cannot be empty.");
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String protocol = parsedUrl.getProtocol().toLowerCase();

            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                return UrlValidationResult.invalid("URL must use HTTP or HTTPS protocol.");
            }

            if (parsedUrl.getHost() == null || parsedUrl.getHost().trim().isEmpty()) {
                return UrlValidationResult.invalid("URL must have a valid host.");
            }

            return UrlValidationResult.valid(url.trim());

        } catch (MalformedURLException e) {
            return UrlValidationResult.invalid("Invalid URL format: " + e.getMessage());
        }
    }

    private String getRandomUserAgent() {
        return USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
    }

    private void configurePageStealth(Page page) {
        page.addInitScript(STEALTH_SCRIPT);
    }

    private void setPageTimeouts(Page page) {
        page.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
        page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
    }

    private String parseWebpageContent(String html, String url) throws ScrapingException {
        try {
            Document doc = Jsoup.parse(html);

            WebpageMetadata metadata = extractMetadata(doc, url);
            String mainContent = extractMainContent(doc);

            return formatWebpageResult(metadata, mainContent);

        } catch (Exception e) {
            throw new ScrapingException("Failed to parse webpage content: " + e.getMessage(), e);
        }
    }

    private WebpageMetadata extractMetadata(Document doc, String url) {
        String title = doc.title();
        String domain = extractDomain(url);
        String description = extractMetaDescription(doc);
        String author = extractAuthor(doc);
        String publishDate = extractPublishDate(doc);

        return new WebpageMetadata(title, domain, description, author, publishDate);
    }

    private String extractMainContent(Document doc) {
        // Remove unwanted elements
        doc.select(REMOVE_ELEMENTS_SELECTORS).remove();

        // Try to find main content area
        Element mainContent = doc.selectFirst(MAIN_CONTENT_SELECTORS);
        if (mainContent != null) {
            return mainContent.text();
        }

        // Fallback to body content
        return doc.body().text();
    }

    private String formatWebpageResult(WebpageMetadata metadata, String content) {
        String result = truncateContent(content, MAX_CONTENT_LENGTH);
        return result;
    }

    private String extractStructuredData(String html, String selector, String url) throws ScrapingException {
        try {
            Document doc = Jsoup.parse(html);

            if (selector != null && !selector.trim().isEmpty()) {
                return extractSpecificElements(doc, selector.trim(), url);
            } else {
                return extractCommonStructuredData(doc, url);
            }

        } catch (Exception e) {
            throw new ScrapingException("Failed to extract structured data: " + e.getMessage(), e);
        }
    }

    private String extractSpecificElements(Document doc, String selector, String url) {
        Elements elements = doc.select(selector);

        if (elements.isEmpty()) {
            return String.format("❌ No elements found with selector: '%s'", selector);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("📊 **Structured Data from %s**\n\n", extractDomain(url)));
        result.append(String.format("🎯 **Selector:** %s\n", selector));
        result.append(String.format("📈 **Found:** %d elements\n\n", elements.size()));

        int count = 0;
        for (Element element : elements) {
            if (count >= MAX_STRUCTURED_ELEMENTS) break;

            String text = element.text();
            if (!text.isEmpty()) {
                result.append(String.format("**%d.** %s\n", count + 1,
                        text.length() > 100 ? text.substring(0, 100) + "..." : text));
                count++;
            }
        }

        return result.toString();
    }

    private String extractCommonStructuredData(Document doc, String url) {
        StringBuilder result = new StringBuilder();
        result.append(String.format("📊 **Structured Data from %s**\n\n", extractDomain(url)));

        // Count different types of structured elements
        int tables = doc.select(TABLE_SELECTOR).size();
        int lists = doc.select(LIST_SELECTOR).size();
        int links = doc.select(LINK_SELECTOR).size();
        int images = doc.select(IMAGE_SELECTOR).size();

        if (tables > 0) result.append(String.format("📋 **Tables:** %d found\n", tables));
        if (lists > 0) result.append(String.format("📝 **Lists:** %d found\n", lists));
        if (links > 0) result.append(String.format("🔗 **Links:** %d found\n", links));
        if (images > 0) result.append(String.format("🖼️ **Images:** %d found\n", images));

        if (tables == 0 && lists == 0 && links == 0 && images == 0) {
            result.append("ℹ️ No common structured data elements found.\n");
        }

        return result.toString();
    }

    private String extractDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String extractMetaDescription(Document doc) {
        Element desc = doc.selectFirst(META_DESCRIPTION_SELECTOR);
        return desc != null ? desc.attr("content") : "";
    }

    private String extractAuthor(Document doc) {
        Element author = doc.selectFirst(AUTHOR_SELECTORS);
        return author != null ? author.text() : "";
    }

    private String extractPublishDate(Document doc) {
        Element date = doc.selectFirst(DATE_SELECTORS);
        return date != null ? (date.hasAttr("datetime") ? date.attr("datetime") : date.text()) : "";
    }

    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }

        int lastSpace = content.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength - CONTENT_TRUNCATE_THRESHOLD) {
            return content.substring(0, lastSpace) + "...\n\n📄 [Content truncated - full text extracted]";
        }
        return content.substring(0, maxLength) + "...\n\n📄 [Content truncated - full text extracted]";
    }

    private String generateContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.warn("Failed to generate content hash", e);
            return "hash-generation-failed";
        }
    }

    // Helper classes
    private static class UrlValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String cleanUrl;

        private UrlValidationResult(boolean valid, String errorMessage, String cleanUrl) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.cleanUrl = cleanUrl;
        }

        static UrlValidationResult valid(String cleanUrl) {
            return new UrlValidationResult(true, null, cleanUrl);
        }

        static UrlValidationResult invalid(String errorMessage) {
            return new UrlValidationResult(false, errorMessage, null);
        }

        boolean isValid() {
            return valid;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        String getCleanUrl() {
            return cleanUrl;
        }
    }

    private static class WebpageMetadata {
        final String title;
        final String domain;
        final String description;
        final String author;
        final String publishDate;

        WebpageMetadata(String title, String domain, String description, String author, String publishDate) {
            this.title = title != null ? title : "";
            this.domain = domain != null ? domain : "Unknown";
            this.description = description != null ? description : "";
            this.author = author != null ? author : "";
            this.publishDate = publishDate != null ? publishDate : "";
        }
    }

    // Custom exceptions
    private static class ScraperInitializationException extends RuntimeException {
        ScraperInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    //Custom Exceptions
    private static class ScrapingException extends Exception {
        ScrapingException(String message) {
            super(message);
        }

        ScrapingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
package com.mcp.webScraper.Workers;

import com.mcp.webScraper.entity.ScrapeResult;
import com.mcp.webScraper.utils.ProxyService_withPearl;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Geolocation;
import com.microsoft.playwright.options.LoadState;

import com.microsoft.playwright.options.WaitUntilState;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.mcp.webScraper.Workers.PlaywrightConfig.*;

/**
 * This service provides the functionality to scrape web pages using Playwright.
 * It can handle both HTML pages and PDF documents.
 */
@Service
public class PlaywrightWebScraperTools {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightWebScraperTools.class);

    // A CSS selector to identify the main content of a web page.
    private static final String MAIN_CONTENT_SELECTOR = "main, article, [role=main], .content, .post, .entry, .blog, .story";

    // Instance variables for Playwright and the browser.
    private volatile Playwright playwright;
    private volatile Browser browser;
    private final SecureRandom random = new SecureRandom();
    private final AtomicLong scrapeCount = new AtomicLong(0);
    private AtomicBoolean isUse = new AtomicBoolean(false);

    private ProxyService_withPearl proxyServiceWithPearl;

    /**
     * Constructor for the PlaywrightWebScraperTools.
     * Initializes the Playwright browser.
     */
    public PlaywrightWebScraperTools() {
        //logger.info("Initializing Playwright scraper tool...");
        //initializeBrowser();
    }

    /**
     * Sets the proxy service for this instance. Can be called after creation.
     * @param proxyService The central proxy service.
     */
    public void setProxyService(ProxyService_withPearl proxyService) {
        this.proxyServiceWithPearl = proxyService;
        logger.debug("Proxy service has been set for this instance.");
        initializeBrowser();
    }

    /**
     * This method is called before the bean is destroyed.
     * It closes the Playwright browser and releases any resources.
     */
    public void cleanup() {
        //logger.info("Shutting down Playwright scraper tool");
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
     * This is the main method for scraping a web page.
     * It takes a URL and returns the scraped content.
     */
    public ScrapeResult scrapeWebpage(String url) {
        if (browser == null) {
            return new ScrapeResult(false, null, "Browser not initialized", url);
        }

        long scrapeId = scrapeCount.incrementAndGet();
        logger.debug("Starting scrape #{}: {}", scrapeId, url);

        if (!isValidUrl(url)) {
            return sendError(url, "Not a valid url!!");
        }

        // If the URL points to a PDF file, use the PDF extraction logic.
        if (url.toLowerCase().endsWith(".pdf")) {
            try {
                logger.debug("Scraping pdf content");
                String pdfContent = extractPdfContent(url);
                return new ScrapeResult(true, pdfContent, null, url);
            } catch (Exception e) {
                logger.error("PDF extraction failed for {}: {}", url, e.getMessage());
                return sendError(url, "PDF site extraction failed!!");
            }
        }

        // For HTML pages, use Playwright to fetch and extract the content.
        try (BrowserContext context = createContext()) {
            Page page = context.newPage();
            setupPage(page);

            return fetchAndExtractContentStructured(page, url, scrapeId);

        } catch (Exception e) {
            logger.error("Scrape #{} failed: {}", scrapeId, e.getMessage());
            return sendError(url, "Something went wrong");
        }
    }


    /**
     * This method fetches the content of a web page with retry logic.
     * It will retry the request up to MAX_RETRIES times if it fails.
     */
    private ScrapeResult fetchAndExtractContentStructured(Page page, String url, long scrapeId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.debug("Scrape #{} attempt {} of {}", scrapeId, attempt, MAX_RETRIES);

                Response response = page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(NAVIGATION_TIMEOUT_MS));

                int status = response != null ? response.status() : 0;
                if (response == null || !response.ok()) {
                    logger.warn("Failed to load page: {}", response != null ? "HTTP " + status : "No response");
                    return sendError(url, "Failed to load page: " + status);
                }

                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_TIMEOUT_MS));
                } catch (Exception ignored) {
                }

                String content = extractContent(page);
                logger.debug("Scraped site successfully: {}", url);
                return new ScrapeResult(true, content, url, null);

            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    return sendError(url, "Failed after " + MAX_RETRIES + " attempts");
                }

                long backoff = (long) WAIT_TIMEOUT_MS * attempt;
                logger.warn("Scrape #{} attempt {} failed: {} – retrying in {} ms",
                        scrapeId, attempt, e.getMessage(), backoff);

                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return sendError(url, "Something went wrong");
    }


    /**
     * This method extracts the main content of a web page.
     * It first tries to extract the visible text from the main content element.
     * If that fails, it falls back to using Jsoup to parse the HTML and extract the text.
     */
    private String extractContent(Page page) {
        try {
            // Wait for the main content to appear on the page.
            Locator mainLocator = page.locator(MAIN_CONTENT_SELECTOR).first();
            try {
                mainLocator.waitFor(new Locator.WaitForOptions().setTimeout(WAIT_TIMEOUT_MS));
            } catch (PlaywrightException e) {
                logger.warn("Main content not found, falling back to full page text");
            }

            // Extract the visible text from the main content element.
            String content = mainLocator.isVisible() ? mainLocator.innerText() : page.innerText("body");

            if (content == null || content.trim().isEmpty()) {
                // If the main content is empty, fall back to using Jsoup to parse the HTML and extract the text.
                Document doc = Jsoup.parse(page.content());
                doc.select("script, style, nav, header, footer, aside, noscript, iframe, img, picture, source").remove();
                Element mainContent = doc.selectFirst(MAIN_CONTENT_SELECTOR);
                content = mainContent != null ? mainContent.text() : doc.body().text();
            }

            // Clean up the whitespace in the content.
            content = content.trim().replaceAll("\\s+", " ");

            // Truncate the content if it is too long.
            if (content.length() > MAX_CONTENT_LENGTH) {
                int lastSpace = content.lastIndexOf(' ', MAX_CONTENT_LENGTH);
                if (lastSpace > MAX_CONTENT_LENGTH - CONTENT_TRUNCATE_THRESHOLD) {
                    content = content.substring(0, lastSpace) + "...";
                } else {
                    content = content.substring(0, MAX_CONTENT_LENGTH) + "...";
                }
            }

            return content;

        } catch (Exception e) {
            logger.error("Content extraction failed: {}", e.getMessage());
            return "Failed to extract content";
        }
    }

    /**
     * This method extracts the text content of a PDF document.
     */
    private String extractPdfContent(String url) throws IOException {
        try (InputStream in = new URL(url).openStream();
             PDDocument document = new PDFParser(new RandomAccessReadBuffer(in)).parse()) {

            if (document.isEncrypted()) {
                throw new IOException("Encrypted PDFs are not supported");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Truncate the content if it is too long.
            if (text.length() > MAX_CONTENT_LENGTH) {
                int lastSpace = text.lastIndexOf(' ', MAX_CONTENT_LENGTH);
                text = text.substring(0, lastSpace > MAX_CONTENT_LENGTH - CONTENT_TRUNCATE_THRESHOLD ? lastSpace : MAX_CONTENT_LENGTH) + "...";
            }

            return text.trim();
        }
    }


    /**
     * This method validates a URL.
     * It checks if the URL is null or empty, and if it has a valid protocol (http or https).
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URL parsedUrl = new URL(url.trim());
            String protocol = parsedUrl.getProtocol().toLowerCase();
            return ("http".equals(protocol) || "https".equals(protocol)) &&
                    parsedUrl.getHost() != null && !parsedUrl.getHost().trim().isEmpty();
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * This method initializes the Playwright browser.
     * It creates a new Playwright instance and launches a Chromium browser.
     */
    private void initializeBrowser() {
        try {
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(BROWSER_HEADLESS)
                    .setTimeout(DEFAULT_TIMEOUT_MS)
                    .setArgs(BROWSER_ARGS);

            // Configure Proxy with Fallback
            if (proxyServiceWithPearl != null) {
                proxyServiceWithPearl.createProxyConfig().ifPresentOrElse(
                        proxy -> {
                            options.setProxy(proxy);
                            logger.debug("Browser initialized with Proxy: {}", proxy.server);
                        },
                        () -> logger.warn("No proxy available. Initializing browser with DIRECT connection.")
                );
            }

            browser = playwright.chromium().launch(options);
            //logger.info("Browser initialized successfully");
        } catch (Exception e) {
            logger.error("Browser initialization failed-> {}", e.getMessage());
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
     * It adds a stealth script to avoid bot detection.
     */
    private void setupPage(Page page) {
        page.addInitScript(STEALTH_SCRIPT);

        // Block resources
        page.route("**/*", route -> {
            String url = route.request().url();
            String resourceType = route.request().resourceType();
            try {
                if (resourceType.equals("image") || resourceType.equals("stylesheet") ||
                        resourceType.equals("font") || resourceType.equals("media") ||
                        url.contains("ads") || url.contains("analytics") ||
                        url.contains("tracking") || url.contains("metrics")) {
                    route.abort();
                } else {
                    route.resume();
                }
            } catch (Exception e) {
                try {
                    route.resume();
                } catch (Exception ignored) {
                }
            }
        });

        page.setDefaultTimeout(DEFAULT_TIMEOUT_MS);
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
     * This method returns a null objects with error message.
     */
    private ScrapeResult sendError(String url, String errorMessage) {
        return new ScrapeResult(false, null, url, errorMessage);
    }
}
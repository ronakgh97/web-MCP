package com.mcp.webScraper.workers;

import java.util.List;
import java.util.Map;

public class PlaywrightConfig {

    // ============= BROWSER CONFIG =============
    public static final int VIEWPORT_WIDTH = 1920;
    public static final int VIEWPORT_HEIGHT = 1080;
    public static final boolean BROWSER_HEADLESS = true;

    // ============= TIMEOUTS =============
    public static final int DEFAULT_TIMEOUT_MS = 30000;
    public static final int NAVIGATION_TIMEOUT_MS = 30000;
    public static final int NETWORK_IDLE_TIMEOUT_MS = 15000;
    public static final int ADDITIONAL_WAIT_MS = 2000;
    public static final int RETRY_DELAY_BASE_MS = 2000;
    public static final int BASE_WAIT_TIME_MS = 3000;
    public static final int RANDOM_WAIT_TIME_MS = 2000;
    public static final long SELECTOR_TIMEOUT_MS = 5000;
    public static final int MAX_RETRY_ATTEMPTS = 2;

    // ============= CONTENT =============
    public static final int MAX_CONTENT_LENGTH = 10000;
    public static final int CONTENT_TRUNCATE_THRESHOLD = 10000;
    public static final int MAX_STRUCTURED_ELEMENTS = 10;

    // ============= POOL CONFIG =============
    public static final int POOL_SIZE = 5;
    public static final int ACQUIRE_TIMEOUT_MS = 10000;

    // User Agents Pool
    public static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/115.0",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36 Edg/118.0.2088.76"
    );

    // ============= HEADERS =============
    static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language", "en-US,en;q=0.9",
            "Accept-Encoding", "gzip, deflate, br",
            "DNT", "1",
            "Connection", "keep-alive",
            "Upgrade-Insecure-Requests", "1",
            "Cache-Control", "no-cache",
            "Pragma", "no-cache"
    );

    // Accept-Language header variations
    public static final List<String> ACCEPT_LANGUAGES = List.of(
            "en-US,en;q=0.9",
            "en-US,en;q=0.9,es;q=0.8",
            "en-GB,en;q=0.9,en-US;q=0.8"
    );

    // Browser launch arguments
    public static final List<String> BROWSER_ARGS = List.of(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-blink-features=AutomationControlled",
            "--disable-extensions",
            "--no-first-run",
            "--disable-default-apps",
            "--disable-infobars",
            "--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT,
            "--memory-pressure-off",
            "--max_old_space_size=4096",
            "--disable-background-networking"
    );

    // Stealth script
    public static final String STEALTH_SCRIPT = """
            () => {
                // Remove webdriver property
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                });
            
                // Mock chrome object  
                window.chrome = {
                    runtime: {},
                    loadTimes: () => ({}),
                    csi: () => ({})
                };
            
                // Mock plugins
                Object.defineProperty(navigator, 'plugins', {
                    get: () => Array.from({length: 5}, () => ({}))
                });
            
                // Mock languages
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['en-US', 'en']
                });
            
                // Hide automation indicators
                Object.defineProperty(navigator, 'permissions', {
                    get: () => ({
                        query: () => Promise.resolve({ state: 'granted' })
                    })
                });
            }
            """;

    public PlaywrightConfig() {
    }
}

package com.mcp.webScraper.Workers;

import com.mcp.webScraper.utils.ProxyService_withPearl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Profile("prod")
@DependsOn({"proxyService_withPearl"})
@Service
public class PlaywrightAllocator { //TODO: Use Thread Pool or ExecutorService implementation for better concurrency handling

    private static final Logger log = LoggerFactory.getLogger(PlaywrightAllocator.class);

    private PlaywrightBrowserSearchTools[] searchTools;
    private PlaywrightWebScraperTools[] scraperTools;

    @Autowired
    private ProxyService_withPearl proxyServiceWithPearl;

    @Value("${playwright.lockInstances:10}")
    private int instances;

    // Usage tracking variables
    private final AtomicInteger activeSearchInstances = new AtomicInteger(0);
    private final AtomicInteger activeScraperInstances = new AtomicInteger(0);
    private final AtomicLong totalSearchBorrows = new AtomicLong(0);
    private final AtomicLong totalScraperBorrows = new AtomicLong(0);
    private final AtomicLong totalSearchWaits = new AtomicLong(0);
    private final AtomicLong totalScraperWaits = new AtomicLong(0);

    // Semaphores for fair resource allocation
    private Semaphore searchSemaphore;
    private Semaphore scraperSemaphore;

    private volatile boolean initialized = false;

    @PostConstruct
    private void init() {
        log.info("Initializing {} Playwright instances...", instances);
        allocate();
    }

    private void allocate() {
        try {
            // Initialize semaphores for resource limiting
            searchSemaphore = new Semaphore(instances, true); // Fair semaphore
            scraperSemaphore = new Semaphore(instances, true);

            searchTools = new PlaywrightBrowserSearchTools[instances];
            scraperTools = new PlaywrightWebScraperTools[instances];

            // Initialize search instances
            int searchSuccessCount = 0;
            for (int i = 0; i < instances; i++) {
                try {
                    searchTools[i] = new PlaywrightBrowserSearchTools();
                    // Set proxy service
                    searchTools[i].setProxyService(proxyServiceWithPearl);
                    log.debug("Search instance {} initialized", i);
                    searchSuccessCount++;
                } catch (Exception e) {
                    log.error("Failed to initialize search instance {}: {}", i, e.getMessage());
                    searchTools[i] = null;
                }
            }

            // Initialize scraper instances
            int scraperSuccessCount = 0;
            for (int i = 0; i < instances; i++) {
                try {
                    scraperTools[i] = new PlaywrightWebScraperTools();
                    // Set proxy service
                    scraperTools[i].setProxyService(proxyServiceWithPearl);
                    log.debug("Scraper instance {} initialized", i);
                    scraperSuccessCount++;
                } catch (Exception e) {
                    log.error("Failed to initialize scraper instance {}: {}", i, e.getMessage());
                    scraperTools[i] = null;
                }
            }

            initialized = true;
            log.info("Playwright allocator initialized - Search: {}/{}, Scraper: {}/{} instances",
                    searchSuccessCount, instances, scraperSuccessCount, instances);

        } catch (Exception e) {
            log.error("Critical failure during instance allocation: {}", e.getMessage());
            initialized = false;
        }
    }

    // Borrow search instance
    public PlaywrightBrowserSearchTools borrowSearchInstance(long requestId) {
        if (!initialized || searchTools == null) {
            log.warn("Allocator not initialized - cannot provide search instance");
            return null;
        }

        long startTime = System.nanoTime();

        try {
            log.debug("Request {} attempting to borrow search instance", requestId);

            // Wait for available instance (with timeout)
            boolean acquired = searchSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                totalSearchWaits.incrementAndGet();
                log.warn("Request {} timed out waiting for search instance", requestId);
                return null;
            }

            // Find and acquire an instance
            PlaywrightBrowserSearchTools instance = findAvailableSearchInstance(requestId);
            if (instance != null) {
                // Atomic lock acquisition
                if (instance.tryAcquire()) {
                    activeSearchInstances.incrementAndGet();
                    totalSearchBorrows.incrementAndGet();

                    long waitTime = System.nanoTime() - startTime;
                    log.info("Request {} borrowed search instance (active: {}, wait: {}ms)",
                            requestId, activeSearchInstances.get(), waitTime / 1_000_000);

                    return instance;
                } else {
                    // Instance was acquired by another thread, release semaphore
                    searchSemaphore.release();
                    log.warn("Request {} failed to acquire search instance lock", requestId);
                }
            } else {
                // No healthy instances, release semaphore
                searchSemaphore.release();
                log.error("Request {} - no healthy search instances available", requestId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Request {} interrupted while waiting for search instance", requestId);
        }

        return null;
    }

    // Return search instance
    public void returnSearchInstance(PlaywrightBrowserSearchTools instance, long requestId) {
        if (instance == null) return;

        try {
            // Release the instance lock
            instance.release();
            activeSearchInstances.decrementAndGet();

            // Release semaphore permit
            searchSemaphore.release();

            log.info("Request {} returned search instance (active: {})",
                    requestId, activeSearchInstances.get());

        } catch (Exception e) {
            log.error("Error returning search instance for request {}: {}", requestId, e.getMessage());
        }
    }

    // Borrow scraper instance
    public PlaywrightWebScraperTools borrowScraperInstance(long requestId) {
        if (!initialized || scraperTools == null) {
            log.warn("Allocator not initialized - cannot provide scraper instance");
            return null;
        }

        long startTime = System.nanoTime();

        try {
            log.debug("Request {} attempting to borrow scraper instance", requestId);

            // Wait for available instance
            boolean acquired = scraperSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                totalScraperWaits.incrementAndGet();
                log.warn("Request {} timed out waiting for scraper instance", requestId);
                return null;
            }

            // Find and acquire an instance
            PlaywrightWebScraperTools instance = findAvailableScraperInstance(requestId);
            if (instance != null) {
                // Atomic lock acquisition
                if (instance.tryAcquire()) {
                    activeScraperInstances.incrementAndGet();
                    totalScraperBorrows.incrementAndGet();

                    long waitTime = System.nanoTime() - startTime;
                    log.info("Request {} borrowed scraper instance (active: {}, wait: {}ms)",
                            requestId, activeScraperInstances.get(), waitTime / 1_000_000);

                    return instance;
                } else {
                    scraperSemaphore.release();
                    log.warn("Request {} failed to acquire scraper instance lock", requestId);
                }
            } else {
                scraperSemaphore.release();
                log.error("Request {} - no healthy scraper instances available", requestId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Request {} interrupted while waiting for scraper instance", requestId);
        }

        return null;
    }

    // Return scraper instance
    public void returnScraperInstance(PlaywrightWebScraperTools instance, long requestId) {
        if (instance == null) return;

        try {
            // Release the instance lock
            instance.release();
            activeScraperInstances.decrementAndGet();

            // Release semaphore permit
            scraperSemaphore.release();

            log.info("Request {} returned scraper instance (active: {})",
                    requestId, activeScraperInstances.get());

        } catch (Exception e) {
            log.error("Error returning scraper instance for request {}: {}", requestId, e.getMessage());
        }
    }

    // Find available search instance
    private PlaywrightBrowserSearchTools findAvailableSearchInstance(long requestId) {
        int startIndex = (int) (requestId % instances);

        // Try round-robin starting from request-based index
        for (int i = 0; i < instances; i++) {
            int index = (startIndex + i) % instances;
            PlaywrightBrowserSearchTools instance = searchTools[index];

            if (instance != null && !instance.isInUse()) {
                log.debug("Request {} selected search instance {}", requestId, index);
                return instance;
            }
        }

        log.warn("Request {} - all search instances are in use or unhealthy", requestId);
        return null;
    }

    // Find available scraper instance
    private PlaywrightWebScraperTools findAvailableScraperInstance(long requestId) {
        int startIndex = (int) (requestId % instances);

        // Try round-robin starting from request-based index
        for (int i = 0; i < instances; i++) {
            int index = (startIndex + i) % instances;
            PlaywrightWebScraperTools instance = scraperTools[index];

            if (instance != null && !instance.isInUse()) {
                log.debug("Request {} selected scraper instance {}", requestId, index);
                return instance;
            }
        }

        log.warn("Request {} - all scraper instances are in use or unhealthy", requestId);
        return null;
    }

    // USAGE INSIGHTS
    public ConcurrentHashMap<String, Double> getUsageStatistics() {

        ConcurrentHashMap<String, Double> stats = new ConcurrentHashMap<>();
        stats.put("Total instances:-", (double) instances);
        stats.put("Searcher used:-", activeSearchInstances.doubleValue());
        stats.put("Scraper used-:", activeScraperInstances.doubleValue());
        stats.put("Utilization %:-", (activeSearchInstances.get() * 100.0 / instances) + (activeScraperInstances.get() * 100.0 / instances));

        return stats;
    }

    // Health check
    public boolean isHealthy() {
        if (!initialized) return false;

        int healthySearch = 0;
        int healthyScraper = 0;

        for (PlaywrightBrowserSearchTools tool : searchTools) {
            if (tool != null) healthySearch++;
        }

        for (PlaywrightWebScraperTools tool : scraperTools) {
            if (tool != null) healthyScraper++;
        }

        return healthySearch > 0 && healthyScraper > 0;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getInstanceCount() {
        return instances;
    }

    public int getActiveSearchInstances() {
        return activeSearchInstances.get();
    }

    public int getActiveScraperInstances() {
        return activeScraperInstances.get();
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up Playwright instances...");
        log.info(getUsageStatistics().toString());

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


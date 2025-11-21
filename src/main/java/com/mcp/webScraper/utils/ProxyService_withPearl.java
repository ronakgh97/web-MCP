package com.mcp.webScraper.utils;

import com.microsoft.playwright.options.Proxy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProxyService_withPearl {

    private static final Logger log = LoggerFactory.getLogger(ProxyService_withPearl.class);

    private static final String PERL_SCRIPT = "scripts/Proxies_Checker.pl";
    private static final String PROXY_SOURCE_FILE = "proxies.txt";
    private static final int maxProxies = 10;
    private static final int testTimeoutSeconds = 5;

    private final AtomicReference<List<String>> validProxies = new AtomicReference<>(new ArrayList<>());
    private final SecureRandom random = new SecureRandom();

    public ProxyService_withPearl() {
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing proxy service...");

        try {
            refreshProxies();
            log.info("Loaded {} fresh proxies", getProxyCount());
        } catch (Exception e) {
            log.error("Failed to load proxies: {}", e.getMessage());
        }
    }

    /**
     * Create proxy configuration for Playwright
     * Perl script output format: IP:PORT:USER:PASS|latency|country|protocols
     */
    public Optional<Proxy> createProxyConfig() {
        List<String> proxies = validProxies.get();
        if (proxies.isEmpty()) {
            log.warn("No proxies available");
            return Optional.empty();
        }

        String proxyLine = proxies.get(random.nextInt(proxies.size()));
        
        // Extract proxy part before pipe (Perl output format)
        String proxyString = proxyLine.split("\\|")[0].trim();
        String[] parts = proxyString.split(":");
        
        if (parts.length == 4) {
            // Authenticated proxy
            String host = parts[0];
            String port = parts[1];
            String username = parts[2];
            String password = parts[3];
            
            log.debug("Using proxy: {}@{}:{}", username, host, port);
            return Optional.of(new Proxy("http://" + host + ":" + port)
                    .setUsername(username)
                    .setPassword(password));
        } else if (parts.length == 2) {
            // Non-authenticated proxy
            log.debug("Using proxy: {}:{}", parts[0], parts[1]);
            return Optional.of(new Proxy("http://" + parts[0] + ":" + parts[1]));
        }
        
        log.warn("Invalid proxy format: {}", proxyString);
        return Optional.empty();
    }

    public boolean isEnabled() {
        return !validProxies.get().isEmpty();
    }

    public int getProxyCount() {
        return validProxies.get().size();
    }

    public List<String> getAllProxies() {
        return new ArrayList<>(validProxies.get());
    }

    public void refreshProxies() throws IOException, InterruptedException {
        log.info("Starting proxy refresh...");
        loadProxies();
        log.info("Refresh completed: {} proxies", getProxyCount());
    }

    private void loadProxies() throws IOException, InterruptedException {
        // PERL VALIDATION - Reads directly from file
        List<String> working = validateWithPerl();

        if (working.isEmpty()) {
            if (!validProxies.get().isEmpty()) {
                log.warn("Perl validation returned 0 proxies. Keeping existing {} proxies as fallback.", validProxies.get().size());
                return;
            } else {
                log.warn("Perl validation returned 0 proxies and no existing proxies available. Server will use DIRECT connection.");
            }
        }

        // Update atomically
        validProxies.set(working);
    }

    /**
     * BLAZING FAST PERL VALIDATION
     * Delegates to external Perl script for maximum performance
     */
    private List<String> validateWithPerl() throws IOException, InterruptedException {
        log.info("Starting Perl validation using source file: {}", PROXY_SOURCE_FILE);

        // Check if Perl script exists
        Path scriptPath = Paths.get(PERL_SCRIPT);
        if (!Files.exists(scriptPath)) {
            throw new FileNotFoundException("Perl script not found: " + PERL_SCRIPT);
        }

        // Build command
        List<String> command = Arrays.asList(
                "perl", scriptPath.toString(),
                "--max", String.valueOf(maxProxies),
                "--timeout", String.valueOf(testTimeoutSeconds),
                "--file", PROXY_SOURCE_FILE
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        //pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Show Perl progress in console

        Process process = pb.start();

        // Collect working proxies from STDOUT
        List<String> workingProxies = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                workingProxies.add(line.trim());
                log.debug("Perl found working proxy: {}", line);
            }
        }

        // Wait for completion
        boolean finished = process.waitFor(300, TimeUnit.SECONDS); // 5 min timeout
        if (!finished) {
            process.destroyForcibly();
            log.warn("Perl validation timed out after 5 minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Perl script exited with code: {}", exitCode);
        }

        log.info("Perl validation complete: found {} working proxies", workingProxies.size());
        return workingProxies;
    }

}
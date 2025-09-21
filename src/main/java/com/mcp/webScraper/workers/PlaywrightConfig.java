package com.mcp.webScraper.workers;

import java.util.List;
import java.util.Map;

public class PlaywrightConfig {

    // BROWSER CONFIG
    public static final int VIEWPORT_WIDTH = 1920;
    public static final int VIEWPORT_HEIGHT = 1080;
    public static final boolean BROWSER_HEADLESS = true;

    // TIMEOUTS
    public static final int DEFAULT_TIMEOUT_MS = 15000;
    public static final int NAVIGATION_TIMEOUT_MS = 12000;
    public static final int SELECTOR_WAIT_TIMEOUT_MS = 8000;
    public static final int ELEMENT_INTERACTION_TIMEOUT_MS = 5000;
    public static final int NETWORK_IDLE_TIMEOUT_MS = 10000;
    public static final int RETRY_DELAY_BASE_MS = 2000;
    public static final int WAIT_TIMEOUT_MS = 3000;
    public static final int MAX_RETRIES = 2;

    // NETWORK RESILIENCE
    public static final int NETWORK_RETRY_ATTEMPTS = 3;
    public static final int CONNECTION_TIMEOUT_MS = 10000;
    public static final int DNS_TIMEOUT_MS = 5000;

    // Network error patterns to retry
    public static final List<String> RETRYABLE_ERRORS = List.of(
            "net::ERR_CONNECTION_REFUSED",
            "net::ERR_CONNECTION_TIMED_OUT",
            "net::ERR_NAME_NOT_RESOLVED",
            "net::ERR_INTERNET_DISCONNECTED",
            "TimeoutError",
            "Protocol error"
    );


    // CONTENT
    public static final int MAX_CONTENT_LENGTH = 10000;
    public static final int CONTENT_TRUNCATE_THRESHOLD = 10000;

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

    // HEADERS
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
            "--disable-extensions",
            "--no-first-run",
            "--disable-default-apps",
            "--disable-infobars",
            "--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT,
            "--memory-pressure-off",
            "--max_old_space_size=4096",
            "--disable-background-networking",
            "--disable-features=VizDisplayCompositor",
            "--disable-background-timer-throttling",
            "--disable-renderer-backgrounding",
            "--disable-backgrounding-occluded-windows",
            "--disable-ipc-flooding-protection",
            "--disable-client-side-phishing-detection",
            "--disable-sync",
            "--disable-background-mode",
            "--force-color-profile=srgb",
            "--metrics-recording-only",
            "--no-crash-upload",
            "--disable-logging",
            "--silent",
            "--disable-blink-features=AutomationControlled",        // Hide automation
            "--exclude-switches=enable-automation",                 // Remove automation switches
            "--disable-extensions-except=",                         // No extensions
            "--disable-plugins-discovery",                          // No plugin discovery
            "--disable-bundled-ppapi-flash",                        // No flash
            "--disable-ipc-flooding-protection",                    // Better performance
            "--disable-hang-monitor",                               // No hang detection
            "--disable-popup-blocking",                             // No popup blocking
            "--disable-prompt-on-repost",                           // No repost prompts
            "--disable-features=TranslateUI",                       // No translate
            "--disable-web-security",                               // Disable CORS (careful!)
            "--allow-running-insecure-content",                     // Allow mixed content
            "--disable-component-extensions-with-background-pages", // No background extensions
            "--run-all-compositor-stages-before-draw",              // Smoother rendering
            "--disable-background-mode",                            // No background mode
            "--no-pings",                                          // No ping requests
            "--no-zygote",                                         // Better isolation
            "--disable-accelerated-2d-canvas",                     // Consistent canvas
            "--disable-accelerated-video-decode"                  // Consistent video
    );

    // Stealth script
    public static final String STEALTH_SCRIPT = """
            () => {
                // === CORE AUTOMATION HIDING ===
            
                // Remove webdriver property completely
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    set: () => {},
                    configurable: true
                });
            
                // Delete automation flags
                delete navigator.__proto__.webdriver;
                delete navigator.webdriver;
            
                // === CHROME OBJECT MOCKING ===
            
                // Comprehensive chrome object
                window.chrome = {
                    app: {
                        isInstalled: false,
                        InstallState: {
                            DISABLED: 'disabled',
                            INSTALLED: 'installed',
                            NOT_INSTALLED: 'not_installed'
                        },
                        RunningState: {
                            CANNOT_RUN: 'cannot_run',
                            READY_TO_RUN: 'ready_to_run', 
                            RUNNING: 'running'
                        }
                    },
                    runtime: {
                        onConnect: null,
                        onMessage: null,
                        onStartup: null,
                        onInstalled: null,
                        onSuspend: null,
                        onSuspendCanceled: null,
                        connect: () => ({}),
                        sendMessage: () => ({}),
                        getManifest: () => ({ version: '1.0.0' }),
                        getURL: (path) => `chrome-extension://fake/${path}`
                    },
                    loadTimes: () => ({
                        commitLoadTime: Math.random() * 1000 + 1000,
                        connectionInfo: 'h2',
                        finishDocumentLoadTime: Math.random() * 1000 + 2000,
                        finishLoadTime: Math.random() * 1000 + 3000,
                        firstPaintAfterLoadTime: Math.random() * 100 + 100,
                        firstPaintTime: Math.random() * 100 + 50,
                        navigationType: 'navigate',
                        numTabsInWindow: Math.floor(Math.random() * 10) + 1,
                        requestTime: Date.now() / 1000 - Math.random() * 1000,
                        startLoadTime: Math.random() * 100,
                        wasAlternateProtocolAvailable: false,
                        wasFetchedViaSpdy: true,
                        wasNpnNegotiated: true
                    }),
                    csi: () => ({
                        onloadT: Date.now(),
                        startE: Date.now() - Math.random() * 1000,
                        tran: Math.floor(Math.random() * 20) + 1
                    })
                };
            
                // === PLUGIN SYSTEM MOCKING ===
            
                // Realistic plugin array
                const mockPlugins = [
                    {
                        0: { type: 'application/x-google-chrome-pdf', suffixes: 'pdf', description: 'Portable Document Format', enabledPlugin: null },
                        description: 'Portable Document Format',
                        filename: 'internal-pdf-viewer',
                        length: 1,
                        name: 'Chrome PDF Plugin'
                    },
                    {
                        0: { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format', enabledPlugin: null },
                        description: 'Portable Document Format', 
                        filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
                        length: 1,
                        name: 'Chrome PDF Viewer'
                    },
                    {
                        0: { type: 'application/x-nacl', suffixes: '', description: 'Native Client Executable', enabledPlugin: null },
                        1: { type: 'application/x-pnacl', suffixes: '', description: 'Portable Native Client Executable', enabledPlugin: null },
                        description: 'Native Client',
                        filename: 'internal-nacl-plugin',
                        length: 2,
                        name: 'Native Client'
                    }
                ];
            
                Object.defineProperty(navigator, 'plugins', {
                    get: () => mockPlugins,
                    configurable: true
                });
            
                // === LANGUAGE & LOCALE MOCKING ===
            
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['en-US', 'en', 'es'],
                    configurable: true
                });
            
                Object.defineProperty(navigator, 'language', {
                    get: () => 'en-US',
                    configurable: true
                });
            
                // === PERMISSIONS API MOCKING ===
            
                const originalQuery = window.navigator.permissions.query;
                window.navigator.permissions.query = (parameters) => (
                    parameters.name === 'notifications' ?
                        Promise.resolve({ state: Notification.permission }) :
                        originalQuery(parameters)
                );
            
                // === WEBRTC IP LEAK PROTECTION ===
            
                const getOrig = RTCPeerConnection.prototype.getStats;
                RTCPeerConnection.prototype.getStats = function(...args) {
                    return getOrig.apply(this, args).then(stats => {
                        // Filter out local IP addresses
                        for (let [id, stat] of stats) {
                            if (stat.type === 'candidate-pair' && stat.localCandidateId) {
                                const localCandidate = stats.get(stat.localCandidateId);
                                if (localCandidate && localCandidate.candidateType === 'host') {
                                    stats.delete(id);
                                    stats.delete(stat.localCandidateId);
                                }
                            }
                        }
                        return stats;
                    });
                };
            
                // === CANVAS FINGERPRINTING PROTECTION ===
            
                const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
            
                // Add subtle noise to canvas operations
                HTMLCanvasElement.prototype.toDataURL = function(...args) {
                    const ctx = this.getContext('2d');
                    const imageData = ctx.getImageData(0, 0, this.width, this.height);
            
                    // Add minimal noise
                    for (let i = 0; i < imageData.data.length; i += 4) {
                        if (Math.random() < 0.001) {
                            imageData.data[i] = Math.min(255, imageData.data[i] + Math.floor(Math.random() * 3) - 1);
                        }
                    }
            
                    ctx.putImageData(imageData, 0, 0);
                    return originalToDataURL.apply(this, args);
                };
            
                // === WEBGL FINGERPRINTING PROTECTION ===
            
                const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(parameter) {
                    // Spoof common WebGL parameters
                    switch (parameter) {
                        case 0x1F00: // GL_VENDOR
                            return 'Google Inc. (NVIDIA)';
                        case 0x1F01: // GL_RENDERER  
                            return 'ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 6GB Direct3D11 vs_5_0 ps_5_0, D3D11)';
                        case 0x1F02: // GL_VERSION
                            return 'OpenGL ES 2.0 (ANGLE 2.1.0.2d07c19f90dc)';
                        case 0x8B8C: // GL_SHADING_LANGUAGE_VERSION
                            return 'OpenGL ES GLSL ES 1.00 (ANGLE 2.1.0.2d07c19f90dc)';
                        default:
                            return originalGetParameter.call(this, parameter);
                    }
                };
            
                // === FONT DETECTION PROTECTION ===
            
                // Override font measurement to prevent fingerprinting
                const originalMeasureText = CanvasRenderingContext2D.prototype.measureText;
                CanvasRenderingContext2D.prototype.measureText = function(text) {
                    const result = originalMeasureText.call(this, text);
                    // Add slight randomization to font measurements
                    result.width += (Math.random() - 0.5) * 0.1;
                    return result;
                };
            
                // === AUDIO CONTEXT FINGERPRINTING PROTECTION ===
            
                const AudioContext = window.AudioContext || window.webkitAudioContext;
                if (AudioContext) {
                    const originalCreateAnalyser = AudioContext.prototype.createAnalyser;
                    AudioContext.prototype.createAnalyser = function() {
                        const analyser = originalCreateAnalyser.call(this);
                        const originalGetFloatFrequencyData = analyser.getFloatFrequencyData;
            
                        analyser.getFloatFrequencyData = function(array) {
                            originalGetFloatFrequencyData.call(this, array);
                            // Add noise to audio fingerprinting
                            for (let i = 0; i < array.length; i++) {
                                array[i] += (Math.random() - 0.5) * 0.0001;
                            }
                        };
            
                        return analyser;
                    };
                }
            
                // === SCREEN FINGERPRINTING PROTECTION ===
            
                // Randomize screen properties slightly
                const screenProps = ['width', 'height', 'availWidth', 'availHeight'];
                screenProps.forEach(prop => {
                    const originalValue = screen[prop];
                    Object.defineProperty(screen, prop, {
                        get: () => originalValue + (Math.random() > 0.5 ? 0 : (Math.random() > 0.5 ? 1 : -1))
                    });
                });
            
                // === TIMEZONE SPOOFING ===
            
                const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
                Date.prototype.getTimezoneOffset = function() {
                    // Spoof to common timezone (EST)
                    return 300;
                };
            
                // === BATTERY API BLOCKING ===
            
                if ('getBattery' in navigator) {
                    navigator.getBattery = () => Promise.resolve({
                        charging: true,
                        chargingTime: Infinity,
                        dischargingTime: Infinity,
                        level: 1.0,
                        addEventListener: () => {},
                        removeEventListener: () => {}
                    });
                }
            
                // === MEMORY INFO SPOOFING ===
            
                if ('memory' in performance) {
                    Object.defineProperty(performance, 'memory', {
                        get: () => ({
                            usedJSHeapSize: 16777216 + Math.floor(Math.random() * 1048576),
                            totalJSHeapSize: 33554432 + Math.floor(Math.random() * 2097152),  
                            jsHeapSizeLimit: 2172649472
                        })
                    });
                }
            
                // === HARDWARE CONCURRENCY SPOOFING ===
            
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => 4 + Math.floor(Math.random() * 5), // 4-8 cores
                    configurable: true
                });
            
                // === CONNECTION INFO SPOOFING ===
            
                if ('connection' in navigator) {
                    Object.defineProperty(navigator, 'connection', {
                        get: () => ({
                            effectiveType: '4g',
                            rtt: 50 + Math.floor(Math.random() * 50),
                            downlink: 10 + Math.random() * 5,
                            addEventListener: () => {},
                            removeEventListener: () => {}
                        })
                    });
                }
            
                // === DEVICE MEMORY SPOOFING ===
            
                if ('deviceMemory' in navigator) {
                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: () => 8, // 8GB RAM
                        configurable: true
                    });
                }
            
                // === USER AGENT CONSISTENCY ===
            
                // Ensure all navigator properties are consistent
                Object.defineProperty(navigator, 'platform', {
                    get: () => 'Win32',
                    configurable: true
                });
            
                Object.defineProperty(navigator, 'vendor', {
                    get: () => 'Google Inc.',
                    configurable: true
                });
            
                // === IFRAME DETECTION BYPASS ===
            
                // Hide iframe context
                Object.defineProperty(window, 'frameElement', {
                    get: () => null,
                    configurable: true
                });
            
                // === PHANTOM JS DETECTION BYPASS ===
            
                if ('callPhantom' in window) {
                    delete window.callPhantom;
                }
            
                if ('_phantom' in window) {
                    delete window._phantom;
                }
            
                // === SELENIUM DETECTION BYPASS ===
            
                if ('__selenium_evaluate' in window) {
                    delete window.__selenium_evaluate;
                }
            
                if ('__selenium_evaluate' in document) {
                    delete document.__selenium_evaluate;
                }
            
                if ('__webdriver_evaluate' in window) {
                    delete window.__webdriver_evaluate;
                }
            
                if ('__driver_evaluate' in window) {
                    delete window.__driver_evaluate;
                }
            
                // === FINAL CLEANUP ===
            
                // Remove any remaining automation traces
                ['__fxdriver_evaluate', '__driver_unwrapped', '__webdriver_unwrapped', 
                 '__driver_evaluate', '__selenium_evaluate', '__fxdriver_unwrapped',
                 '_Selenium_IDE_Recorder', '_selenium', 'calledSelenium', '$cdc_asdjflasutopfhvcZLmcfl_',
                 '$chrome_asyncScriptInfo', '__$webdriverAsyncExecutor'].forEach(prop => {
                    if (prop in window) {
                        delete window[prop];
                    }
                });
            
                // Prevent future webdriver property assignment
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    set: () => {},
                    configurable: false,
                    enumerable: false
                });
            
                console.log('Stealth mode activated');
            }
            """;


    public PlaywrightConfig() {
    }
}

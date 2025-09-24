package com.mcp.webScraper.Workers;

import java.util.Arrays;
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

    // Accept-Language header variations
    public static final List<String> ACCEPT_LANGUAGES = List.of(
            "en-US,en;q=0.9",
            "en-US,en;q=0.9,es;q=0.8",
            "en-GB,en;q=0.9,en-US;q=0.8"
    );

    // CONTENT
    public static final int MAX_CONTENT_LENGTH = 5500;
    public static final int CONTENT_TRUNCATE_THRESHOLD = 5000;

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

    static final List<LocationProfile> RESIDENTIAL_PROFILES = Arrays.asList(
            new LocationProfile("America/New_York", 40.7128, -74.0060, "en-US", "New York, NY"),
            new LocationProfile("America/Los_Angeles", 34.0522, -118.2437, "en-US", "Los Angeles, CA"),
            new LocationProfile("America/Chicago", 41.8781, -87.6298, "en-US", "Chicago, IL"),
            new LocationProfile("America/Denver", 39.7392, -104.9903, "en-US", "Denver, CO"),
            new LocationProfile("Europe/London", 51.5074, -0.1278, "en-GB", "London, UK"),
            new LocationProfile("Europe/Berlin", 52.5200, 13.4050, "de-DE", "Berlin, Germany"),
            new LocationProfile("Asia/Tokyo", 35.6762, 139.6503, "ja-JP", "Tokyo, Japan"),
            new LocationProfile("Australia/Sydney", -33.8688, 151.2093, "en-AU", "Sydney, Australia")
    );

    // Browser launch arguments
    public static final List<String> BROWSER_ARGS = List.of(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-save-password-bubble",
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
            "--use-fake-ui-for-media-stream",
            "--use-fake-device-for-media-stream",
            "--autoplay-policy=no-user-gesture-required",
            "--ignore-certificate-errors",
            "--ignore-ssl-errors",
            "--ignore-certificate-errors-spki-list",
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
            "--disable-web-security",                               // Disable CORS
            "--allow-running-insecure-content",                     // Allow mixed content
            "--disable-component-extensions-with-background-pages", // No background extensions
            "--run-all-compositor-stages-before-draw",              // Smoother rendering
            "--disable-background-mode",                            // No background mode
            "--no-pings",                                          // No ping requests
            "--no-zygote",                                         // Better isolation
            "--disable-accelerated-2d-canvas",                     // Consistent canvas
            "--disable-accelerated-video-decode"                  // Consistent video
    );

    public static final String STEALTH_SCRIPT = """
            () => {
                // === CORE AUTOMATION HIDING ===
            
                // Remove webdriver property completely
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                    set: () => {},
                    configurable: false,
                    enumerable: false
                });
            
                // Delete automation flags
                delete navigator.__proto__.webdriver;
                delete navigator.webdriver;
            
                // === CHROME OBJECT MOCKING ===
            
                // Comprehensive chrome object with randomized data
                const chromeVersion = `${Math.floor(Math.random() * 10) + 90}.0.${Math.floor(Math.random() * 5000)}.${Math.floor(Math.random() * 200)}`;
            
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
                        getManifest: () => ({ version: chromeVersion }),
                        getURL: (path) => `chrome-extension://${Math.random().toString(36).substring(7)}/${path}`
                    },
                    loadTimes: () => ({
                        commitLoadTime: Math.random() * 1000 + 1000,
                        connectionInfo: Math.random() > 0.5 ? 'h2' : 'http/1.1',
                        finishDocumentLoadTime: Math.random() * 1000 + 2000,
                        finishLoadTime: Math.random() * 1000 + 3000,
                        firstPaintAfterLoadTime: Math.random() * 100 + 100,
                        firstPaintTime: Math.random() * 100 + 50,
                        navigationType: 'navigate',
                        numTabsInWindow: Math.floor(Math.random() * 8) + 1,
                        requestTime: Date.now() / 1000 - Math.random() * 1000,
                        startLoadTime: Math.random() * 100,
                        wasAlternateProtocolAvailable: Math.random() > 0.5,
                        wasFetchedViaSpdy: Math.random() > 0.3,
                        wasNpnNegotiated: Math.random() > 0.7
                    }),
                    csi: () => ({
                        onloadT: Date.now() + Math.random() * 100,
                        startE: Date.now() - Math.random() * 1000,
                        tran: Math.floor(Math.random() * 20) + 1
                    })
                };
            
                // === PLUGIN SYSTEM MOCKING ===
            
                // Realistic plugin array with slight randomization
                const pluginVariations = [
                    ['Chrome PDF Plugin', 'Chrome PDF Viewer', 'Native Client'],
                    ['Chromium PDF Plugin', 'Chromium PDF Viewer', 'Native Client'],
                    ['Chrome PDF Plugin', 'PDF Viewer', 'Native Client']
                ];
            
                const selectedPlugins = pluginVariations[Math.floor(Math.random() * pluginVariations.length)];
            
                const mockPlugins = [
                    {
                        0: { type: 'application/x-google-chrome-pdf', suffixes: 'pdf', description: 'Portable Document Format', enabledPlugin: null },
                        description: 'Portable Document Format',
                        filename: 'internal-pdf-viewer',
                        length: 1,
                        name: selectedPlugins[0]
                    },
                    {
                        0: { type: 'application/pdf', suffixes: 'pdf', description: 'Portable Document Format', enabledPlugin: null },
                        description: 'Portable Document Format', 
                        filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai',
                        length: 1,
                        name: selectedPlugins[1]
                    },
                    {
                        0: { type: 'application/x-nacl', suffixes: '', description: 'Native Client Executable', enabledPlugin: null },
                        1: { type: 'application/x-pnacl', suffixes: '', description: 'Portable Native Client Executable', enabledPlugin: null },
                        description: 'Native Client',
                        filename: 'internal-nacl-plugin',
                        length: 2,
                        name: selectedPlugins[2]
                    }
                ];
            
                // Add random plugin availability (some users disable plugins)
                const availablePlugins = mockPlugins.filter(() => Math.random() > 0.1);
            
                Object.defineProperty(navigator, 'plugins', {
                    get: () => availablePlugins,
                    configurable: true
                });
            
                // === LANGUAGE & LOCALE MOCKING ===
            
                const languageSets = [
                    ['en-US', 'en'],
                    ['en-US', 'en', 'es'],
                    ['en-GB', 'en'],
                    ['en-US', 'en', 'fr'],
                    ['en-CA', 'en', 'fr']
                ];
            
                const selectedLanguages = languageSets[Math.floor(Math.random() * languageSets.length)];
            
                Object.defineProperty(navigator, 'languages', {
                    get: () => selectedLanguages,
                    configurable: true
                });
            
                Object.defineProperty(navigator, 'language', {
                    get: () => selectedLanguages[0],
                    configurable: true
                });
            
                // === PERMISSIONS API MOCKING ===
            
                if (navigator.permissions && navigator.permissions.query) {
                    const originalQuery = navigator.permissions.query;
                    navigator.permissions.query = (parameters) => {
                        if (parameters.name === 'notifications') {
                            const states = ['granted', 'denied', 'default'];
                            return Promise.resolve({ 
                                state: states[Math.floor(Math.random() * states.length)] 
                            });
                        }
                        return originalQuery(parameters);
                    };
                }
            
                // === WEBRTC IP LEAK PROTECTION ===
            
                if (window.RTCPeerConnection) {
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
                }
            
                // === CANVAS FINGERPRINTING PROTECTION ===
            
                const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function(...args) {
                    // Only add noise sometimes (more realistic)
                    if (Math.random() < 0.7) {
                        const ctx = this.getContext('2d');
                        const imageData = ctx.getImageData(0, 0, this.width, this.height);
            
                        // Add minimal noise
                        for (let i = 0; i < imageData.data.length; i += 4) {
                            if (Math.random() < 0.001) {
                                const noise = Math.floor(Math.random() * 3) - 1;
                                imageData.data[i] = Math.max(0, Math.min(255, imageData.data[i] + noise));
                            }
                        }
            
                        ctx.putImageData(imageData, 0, 0);
                    }
                    return originalToDataURL.apply(this, args);
                };
            
                // === WEBGL FINGERPRINTING PROTECTION ===
            
                // Randomized GPU signatures
                const gpuSignatures = [
                    'ANGLE (NVIDIA, NVIDIA GeForce GTX 1660 6GB Direct3D11 vs_5_0 ps_5_0, D3D11)',
                    'ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0, D3D11)',
                    'ANGLE (AMD, AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0, D3D11)',
                    'ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)'
                ];
            
                const selectedGPU = gpuSignatures[Math.floor(Math.random() * gpuSignatures.length)];
            
                const originalGetParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(parameter) {
                    switch (parameter) {
                        case 0x1F00: // GL_VENDOR
                            return 'Google Inc. (NVIDIA)';
                        case 0x1F01: // GL_RENDERER  
                            return selectedGPU;
                        case 0x1F02: // GL_VERSION
                            return 'OpenGL ES 2.0 (ANGLE 2.1.0.2d07c19f90dc)';
                        case 0x8B8C: // GL_SHADING_LANGUAGE_VERSION
                            return 'OpenGL ES GLSL ES 1.00 (ANGLE 2.1.0.2d07c19f90dc)';
                        default:
                            return originalGetParameter.call(this, parameter);
                    }
                };
            
                // Add WebGL2 support
                if (window.WebGL2RenderingContext) {
                    const originalGetParameter2 = WebGL2RenderingContext.prototype.getParameter;
                    WebGL2RenderingContext.prototype.getParameter = function(parameter) {
                        switch (parameter) {
                            case 0x1F00: return 'Google Inc. (NVIDIA)';
                            case 0x1F01: return selectedGPU;
                            case 0x1F02: return 'OpenGL ES 3.0 (ANGLE 2.1.0.2d07c19f90dc)';
                            case 0x8B8C: return 'OpenGL ES GLSL ES 3.00 (ANGLE 2.1.0.2d07c19f90dc)';
                            default: return originalGetParameter2.call(this, parameter);
                        }
                    };
                }
            
                // === FONT DETECTION PROTECTION ===
            
                const originalMeasureText = CanvasRenderingContext2D.prototype.measureText;
                CanvasRenderingContext2D.prototype.measureText = function(text) {
                    const result = originalMeasureText.call(this, text);
                    // Smaller, more realistic randomization
                    if (Math.random() < 0.3) {
                        result.width += (Math.random() - 0.5) * 0.05;
                    }
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
                            // Even smaller audio noise
                            if (Math.random() < 0.2) {
                                for (let i = 0; i < array.length; i++) {
                                    array[i] += (Math.random() - 0.5) * 0.00001;
                                }
                            }
                        };
            
                        return analyser;
                    };
                }
            
                // === SCREEN FINGERPRINTING PROTECTION ===
            
                // More realistic screen variations
                const screenProps = ['width', 'height', 'availWidth', 'availHeight'];
                screenProps.forEach(prop => {
                    const originalValue = screen[prop];
                    Object.defineProperty(screen, prop, {
                        get: () => {
                            // Only add variation 20% of the time
                            if (Math.random() < 0.2) {
                                return originalValue + (Math.random() > 0.5 ? 1 : -1);
                            }
                            return originalValue;
                        }
                    });
                });
            
                // === TIMEZONE SPOOFING ===
            
                // Randomized common timezones
                const timezones = [
                    300,   // EST (UTC-5)
                    360,   // CST (UTC-6)  
                    420,   // MST (UTC-7)
                    480,   // PST (UTC-8)
                    0,     // GMT (UTC+0)
                    -60,   // CET (UTC+1)
                    -120   // EET (UTC+2)
                ];
            
                const selectedTimezone = timezones[Math.floor(Math.random() * timezones.length)];
            
                const originalGetTimezoneOffset = Date.prototype.getTimezoneOffset;
                Date.prototype.getTimezoneOffset = function() {
                    return selectedTimezone;
                };
            
                // === BATTERY API BLOCKING ===
            
                if ('getBattery' in navigator) {
                    navigator.getBattery = () => Promise.resolve({
                        charging: Math.random() > 0.5,
                        chargingTime: Math.random() > 0.5 ? Infinity : Math.random() * 7200,
                        dischargingTime: Math.random() * 28800 + 3600,
                        level: 0.3 + Math.random() * 0.7,
                        addEventListener: () => {},
                        removeEventListener: () => {}
                    });
                }
            
                // === MEMORY INFO SPOOFING ===
            
                if ('memory' in performance) {
                    Object.defineProperty(performance, 'memory', {
                        get: () => ({
                            usedJSHeapSize: 16777216 + Math.floor(Math.random() * 8388608),
                            totalJSHeapSize: 33554432 + Math.floor(Math.random() * 16777216),  
                            jsHeapSizeLimit: 2172649472 + Math.floor(Math.random() * 536870912)
                        })
                    });
                }
            
                // === HARDWARE CONCURRENCY SPOOFING ===
            
                // More realistic CPU core distribution
                const coreOptions = [2, 4, 6, 8, 12, 16];
                const weights = [0.1, 0.4, 0.2, 0.2, 0.05, 0.05];
                let random = Math.random();
                let selectedCores = 4; // default
            
                for (let i = 0; i < coreOptions.length; i++) {
                    if (random < weights[i]) {
                        selectedCores = coreOptions[i];
                        break;
                    }
                    random -= weights[i];
                }
            
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => selectedCores,
                    configurable: true
                });
            
                // === CONNECTION INFO SPOOFING ===
            
                if ('connection' in navigator) {
                    const connectionTypes = ['4g', '3g', 'wifi', 'ethernet'];
                    const selectedConnection = connectionTypes[Math.floor(Math.random() * connectionTypes.length)];
            
                    Object.defineProperty(navigator, 'connection', {
                        get: () => ({
                            effectiveType: selectedConnection,
                            rtt: 50 + Math.floor(Math.random() * 100),
                            downlink: selectedConnection === '4g' ? 10 + Math.random() * 20 : 2 + Math.random() * 8,
                            addEventListener: () => {},
                            removeEventListener: () => {}
                        })
                    });
                }
            
                // === DEVICE MEMORY SPOOFING ===
            
                if ('deviceMemory' in navigator) {
                    const memoryOptions = [2, 4, 8, 16, 32];
                    const memoryWeights = [0.1, 0.3, 0.4, 0.15, 0.05];
                    let random = Math.random();
                    let selectedMemory = 8;
            
                    for (let i = 0; i < memoryOptions.length; i++) {
                        if (random < memoryWeights[i]) {
                            selectedMemory = memoryOptions[i];
                            break;
                        }
                        random -= memoryWeights[i];
                    }
            
                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: () => selectedMemory,
                        configurable: true
                    });
                }
            
                // === USER AGENT CONSISTENCY ===
            
                // Match platform to user agent
                const platformVariations = ['Win32', 'MacIntel', 'Linux x86_64'];
                const selectedPlatform = platformVariations[Math.floor(Math.random() * platformVariations.length)];
            
                Object.defineProperty(navigator, 'platform', {
                    get: () => selectedPlatform,
                    configurable: true
                });
            
                Object.defineProperty(navigator, 'vendor', {
                    get: () => Math.random() > 0.1 ? 'Google Inc.' : '', // Some browsers have empty vendor
                    configurable: true
                });
            
                // === IFRAME DETECTION BYPASS ===
            
                Object.defineProperty(window, 'frameElement', {
                    get: () => null,
                    configurable: true
                });
            
                // === AUTOMATION DETECTION CLEANUP ===
            
                const automationProps = [
                    'callPhantom', '_phantom', '__selenium_evaluate', '__webdriver_evaluate',
                    '__driver_evaluate', '__fxdriver_evaluate', '__driver_unwrapped', 
                    '__webdriver_unwrapped', '__fxdriver_unwrapped', '_Selenium_IDE_Recorder', 
                    '_selenium', 'calledSelenium', '$cdc_asdjflasutopfhvcZLmcfl_',
                    '$chrome_asyncScriptInfo', '__$webdriverAsyncExecutor', '__nightmare', 
                    '__phantomas'
                ];
            
                automationProps.forEach(prop => {
                    if (prop in window) delete window[prop];
                    if (prop in document) delete document[prop];
            
                    // Prevent re-assignment
                    Object.defineProperty(window, prop, {
                        get: () => undefined,
                        set: () => {},
                        configurable: false,
                        enumerable: false
                    });
                });
            
                // === FIXED BEHAVIORAL SIMULATION ===
            
                let lastMouseMove = Date.now();
                let mouseX = Math.random() * window.innerWidth;
                let mouseY = Math.random() * window.innerHeight;
            
                // Single interval, no nesting
                const simulateMouseActivity = () => {
                    if (Date.now() - lastMouseMove > 8000 + Math.random() * 12000) {
                        mouseX += (Math.random() - 0.5) * 50;
                        mouseY += (Math.random() - 0.5) * 50;
                        mouseX = Math.max(0, Math.min(window.innerWidth, mouseX));
                        mouseY = Math.max(0, Math.min(window.innerHeight, mouseY));
            
                        try {
                            document.dispatchEvent(new MouseEvent('mousemove', {
                                clientX: Math.floor(mouseX),
                                clientY: Math.floor(mouseY),
                                bubbles: true,
                                cancelable: true
                            }));
                        } catch (e) {
                            // Ignore errors in case MouseEvent is not available
                        }
            
                        lastMouseMove = Date.now();
                    }
                };
            
                // Single interval setup
                setInterval(simulateMouseActivity, 5000 + Math.random() * 3000);
            
                // === ENHANCED FETCH OVERRIDE ===
            
                const originalFetch = window.fetch;
                window.fetch = function(...args) {
                    // More realistic network delays
                    const networkDelay = Math.random() * 150 + 25;
            
                    return new Promise((resolve, reject) => {
                        setTimeout(() => {
                            originalFetch.apply(this, args)
                                .then(response => {
                                    const processingDelay = Math.random() * 30 + 5;
                                    setTimeout(() => resolve(response), processingDelay);
                                })
                                .catch(reject);
                        }, networkDelay);
                    });
                };
            
                // === WINDOW FOCUS SIMULATION ===
            
                let focused = true;
                setInterval(() => {
                    if (Math.random() < 0.05) { // 5% chance to toggle focus
                        focused = !focused;
                        try {
                            document.dispatchEvent(new Event(focused ? 'focus' : 'blur'));
                            window.dispatchEvent(new Event(focused ? 'focus' : 'blur'));
                        } catch (e) {
                            // Ignore event errors
                        }
                    }
                }, 45000 + Math.random() * 30000);
            
                // Add Speech Synthesis spoofing
                if ('speechSynthesis' in window && window.speechSynthesis.getVoices) {
                    const originalGetVoices = window.speechSynthesis.getVoices;
                    window.speechSynthesis.getVoices = function() {
                        const voices = originalGetVoices.call(this);
                        // Return real voices if available, otherwise mock
                        if (voices.length > 0) return voices;
            
                        return [{
                            name: 'Microsoft David Desktop',
                            lang: 'en-US',
                            localService: true,
                            default: true,
                            voiceURI: 'Microsoft David Desktop'
                        }];
                    };
                }
            
                console.log('stealth mode');
            }
            """;

    static class LocationProfile {
        private String timezone;
        private double lat, lon;
        private String locale;
        private String city;

        public LocationProfile() {
        }

        LocationProfile(String timezone, double lat, double lon, String locale, String city) {
            this.timezone = timezone;
            this.lat = lat;
            this.lon = lon;
            this.locale = locale;
            this.city = city;
        }

        public String getTimezone() {
            return timezone;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public String getLocale() {
            return locale;
        }

        public String getCity() {
            return city;
        }
    }

    public PlaywrightConfig() {
    }
}

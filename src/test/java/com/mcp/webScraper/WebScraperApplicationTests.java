package com.mcp.webScraper;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class WebScraperApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(WebScraperApplicationTests.class);

    @Test
    void contextLoads() {
        assertTrue(true);
    }

}

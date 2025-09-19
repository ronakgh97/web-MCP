package com.mcp.webScraper;

import com.mcp.webScraper.entries.SearchResult;
import com.mcp.webScraper.workers.PlaywrightBrowserSearchTools;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class WebScraperApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(WebScraperApplicationTests.class);

	@Test
	void contextLoads() {

	}

}

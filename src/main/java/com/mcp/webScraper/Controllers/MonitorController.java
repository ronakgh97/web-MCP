package com.mcp.webScraper.Controllers;

import com.mcp.webScraper.Workers.PlaywrightAllocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("prod")
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    @Autowired(required = false)
    private PlaywrightAllocator playwrightAllocator;

    public ResponseEntity<?> reportMonitor() {
        if (playwrightAllocator.isHealthy())
            return new ResponseEntity<>(playwrightAllocator.getUsageStatistics(), HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }
}

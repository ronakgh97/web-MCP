package com.mcp.webScraper.services;

import com.mcp.webScraper.workers.PlaywrightBrowserSearchTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchSerivces {

    @Autowired
    private PlaywrightBrowserSearchTools playwrightBrowserSearchTools;


}

package com.mcp.webScraper.controllers;

import com.mcp.webScraper.entries.RequestEntries;
import com.mcp.webScraper.entries.ResponseEntries;
import com.mcp.webScraper.workers.PlaywrightBrowserSearchTools;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/search")
public class SearchController {


    private final PlaywrightBrowserSearchTools playwrightBrowserSearchTools;

    public SearchController(PlaywrightBrowserSearchTools playwrightBrowserSearchTools) {
        this.playwrightBrowserSearchTools = playwrightBrowserSearchTools;
    }

    @PostMapping
    public ResponseEntity<ResponseEntries> search(@Valid @RequestBody RequestEntries requestEntries){
        ResponseEntries responseEntries = new ResponseEntries();
        responseEntries.setUserQuery(requestEntries.getQuery());
        responseEntries.setSearchResultList(playwrightBrowserSearchTools.playwrightSearch(requestEntries.getQuery(), "duckduckgo"));

        if(responseEntries.getSearchResultList().isEmpty()) {
            responseEntries.setMessage("ERROR");
            return new ResponseEntity<>(responseEntries, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseEntries.setMessage("SUCCESS");
        return new ResponseEntity<ResponseEntries>(responseEntries, HttpStatus.OK);
    }
}

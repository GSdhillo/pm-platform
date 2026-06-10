package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.application.SearchService;
import com.gurjeet.pm.common.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) { this.searchService = searchService; }

    @GetMapping
    public Map<String, Object> search(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String query,
                                      @RequestParam(required = false) UUID projectId,
                                      @RequestParam(required = false) String cursor,
                                      @RequestParam(defaultValue = "20") int limit,
                                      @AuthenticationPrincipal AuthUser user) {
        var page = searchService.search(user.id(), q, query, projectId, cursor, limit);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hits", page.hits());
        response.put("nextCursor", page.nextCursor());
        return response;
    }
}

package com.dhruvsharma.feed.timelineservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.feed.timelineservice.dto.TimelineResponse;
import com.dhruvsharma.feed.timelineservice.service.TimelineService;

@RestController
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /**
     * Returns the timeline of all posts. The authenticated user is identified
     * from the JWT token (Keycloak subject claim).
     */
    @GetMapping("/api/v1/timelines")
    public TimelineResponse getTimeline(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return timelineService.getTimeline(userId);
    }
}

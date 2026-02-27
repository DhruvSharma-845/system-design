package com.dhruvsharma.feed.timelineservice.service;

import com.dhruvsharma.feed.timelineservice.dto.TimelineResponse;

public interface TimelineService {
    TimelineResponse getTimeline(String userId);
}

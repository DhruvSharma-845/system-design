package com.dhruvsharma.feed.timelineservice.service.impl;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.dhruvsharma.feed.timelineservice.dto.PostDTO;
import com.dhruvsharma.feed.timelineservice.dto.TimelineResponse;
import com.dhruvsharma.feed.timelineservice.repository.TimelineRepository;
import com.dhruvsharma.feed.timelineservice.repository.entity.Post;
import com.dhruvsharma.feed.timelineservice.service.TimelineService;

@Service
public class TimelineServiceImpl implements TimelineService {
    private final TimelineRepository timelineRepository;

    public TimelineServiceImpl(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    @Override
    public TimelineResponse getTimeline(String userId) {
        List<PostDTO> posts = StreamSupport.stream(timelineRepository.findAll().spliterator(), false)
                .map(this::toPostDTO)
                .toList();
        return new TimelineResponse(posts);
    }

    private PostDTO toPostDTO(Post post) {
        return new PostDTO(post.getId(), post.getContent(), post.getAuthorId());
    }
}

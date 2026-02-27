package com.dhruvsharma.feed.timelineservice.repository;

import org.springframework.data.repository.CrudRepository;

import com.dhruvsharma.feed.timelineservice.repository.entity.Post;

public interface TimelineRepository extends CrudRepository<Post, Long> {
}

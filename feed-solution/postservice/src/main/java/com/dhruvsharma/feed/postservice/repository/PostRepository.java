package com.dhruvsharma.feed.postservice.repository;

import org.springframework.data.repository.CrudRepository;

import com.dhruvsharma.feed.postservice.repository.entity.Post;

public interface PostRepository extends CrudRepository<Post, Long> {
}

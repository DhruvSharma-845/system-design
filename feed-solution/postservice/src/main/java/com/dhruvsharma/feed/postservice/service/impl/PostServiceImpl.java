package com.dhruvsharma.feed.postservice.service.impl;

import org.springframework.stereotype.Service;

import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;
import com.dhruvsharma.feed.postservice.repository.PostRepository;
import com.dhruvsharma.feed.postservice.repository.entity.Post;
import com.dhruvsharma.feed.postservice.service.PostService;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;

    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public PostCreationResponse createPost(PostRequest post) {
        Post savedPost = postRepository.save(new Post(null, post.getContent()));
        return new PostCreationResponse(savedPost.getId());
    }
}

package com.dhruvsharma.feed.postservice.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dhruvsharma.feed.postservice.client.UserServiceClient;
import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;
import com.dhruvsharma.feed.postservice.repository.PostRepository;
import com.dhruvsharma.feed.postservice.repository.entity.Post;
import com.dhruvsharma.feed.postservice.service.PostService;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;

    public PostServiceImpl(PostRepository postRepository, UserServiceClient userServiceClient) {
        this.postRepository = postRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public PostCreationResponse createPost(PostRequest post, String authorizationHeader) {
        Long authorUserId = userServiceClient.getCurrentUser(authorizationHeader)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "User not registered. Please log in again to complete registration."));
        Post savedPost = postRepository.save(new Post(null, post.getContent(), authorUserId));
        return new PostCreationResponse(savedPost.getId());
    }
}

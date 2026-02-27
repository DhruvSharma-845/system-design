package com.dhruvsharma.feed.postservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;
import com.dhruvsharma.feed.postservice.service.PostService;

@RestController
public class PostSubmissionController {

    private final PostService postService;

    public PostSubmissionController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Creates a new post. Author is resolved via userservice (JWT forwarded) to get
     * internal user id (Pattern B); that id is persisted in posts.author_id.
     */
    @PostMapping("/api/v1/posts")
    public PostCreationResponse createPost(@RequestBody PostRequest post,
                                           @RequestHeader("Authorization") String authorization) {
        return postService.createPost(post, authorization);
    }
}

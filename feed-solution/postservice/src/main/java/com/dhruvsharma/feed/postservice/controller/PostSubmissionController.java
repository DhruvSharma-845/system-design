package com.dhruvsharma.feed.postservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * Creates a new post. The author is automatically extracted
     * from the authenticated user's JWT token (Keycloak subject claim).
     */
    @PostMapping("/api/v1/posts")
    public PostCreationResponse createPost(@RequestBody PostRequest post,
                                           @AuthenticationPrincipal Jwt jwt) {
        String authorId = jwt.getSubject();
        return postService.createPost(post, authorId);
    }
}

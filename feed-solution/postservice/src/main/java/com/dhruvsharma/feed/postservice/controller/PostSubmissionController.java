package com.dhruvsharma.feed.postservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;
import com.dhruvsharma.feed.postservice.service.PostService;

@RestController
public class PostSubmissionController {

    @Autowired
    private PostService postService;

    @PostMapping("/api/v1/posts")
    public PostCreationResponse createPost(@RequestBody PostRequest post) {
        return postService.createPost(post);
    }
}

package com.dhruvsharma.feed.postservice.service;

import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;

public interface PostService {
    /**
     * Creates a post. Resolves the author via userservice (forwarded JWT) to get internal user id (Pattern B).
     */
    PostCreationResponse createPost(PostRequest post, String authorizationHeader);
}

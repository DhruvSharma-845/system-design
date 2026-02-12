package com.dhruvsharma.feed.postservice.service;

import com.dhruvsharma.feed.postservice.dto.PostCreationResponse;
import com.dhruvsharma.feed.postservice.dto.PostRequest;

public interface PostService {
    PostCreationResponse createPost(PostRequest post);
}

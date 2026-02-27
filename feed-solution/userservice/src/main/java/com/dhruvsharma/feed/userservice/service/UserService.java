package com.dhruvsharma.feed.userservice.service;

import java.util.Optional;

import com.dhruvsharma.feed.userservice.dto.UserResponse;
import com.dhruvsharma.feed.userservice.dto.UserSignupRequest;
import com.dhruvsharma.feed.userservice.dto.UserSignupResponse;

public interface UserService {
    /**
     * Registers the authenticated user in the application database.
     * Idempotent: if the user already exists (by Keycloak sub), returns existing user info.
     */
    UserSignupResponse signup(UserSignupRequest userSignupRequest);

    /**
     * Returns user details by Keycloak subject (sub). Used by other services or frontend
     * to resolve author display info (e.g. for timeline posts).
     */
    Optional<UserResponse> findByKeycloakSubId(String keycloakSubId);

    /**
     * Returns user details by internal user id (Pattern B). Used when timeline/posts
     * store author_user_id and need to resolve to display name.
     */
    Optional<UserResponse> findById(Long id);
}

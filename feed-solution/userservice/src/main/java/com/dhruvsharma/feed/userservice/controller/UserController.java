package com.dhruvsharma.feed.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.feed.userservice.dto.UserResponse;
import com.dhruvsharma.feed.userservice.dto.UserSignupRequest;
import com.dhruvsharma.feed.userservice.dto.UserSignupResponse;
import com.dhruvsharma.feed.userservice.service.UserService;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers the authenticated user in the application database.
     * Called by the frontend after Keycloak login/signup callback.
     * Idempotent: creates the user if not present, otherwise returns existing.
     */
    @PostMapping("/api/v1/users/signup")
    public UserSignupResponse signup(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return userService.signup(new UserSignupRequest(sub, username, email));
    }

    /**
     * Returns the authenticated user (Pattern B: internal user id and profile).
     * Other services forward the caller's JWT to resolve token → internal user id.
     */
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.findByKeycloakSubId(jwt.getSubject())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns user details by internal user id (Pattern B).
     * Used when timeline/posts return author_user_id and client needs display name.
     */
    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

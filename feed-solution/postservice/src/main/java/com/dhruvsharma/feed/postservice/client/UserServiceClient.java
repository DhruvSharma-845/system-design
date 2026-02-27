package com.dhruvsharma.feed.postservice.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.dhruvsharma.feed.postservice.client.dto.UserResponse;

/**
 * Calls userservice to resolve the authenticated user (Pattern B).
 * Forwards the caller's JWT so userservice returns the internal user id.
 */
@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public UserServiceClient(RestTemplate restTemplate,
                            @Value("${feed.userservice.url}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl.endsWith("/") ? userServiceBaseUrl : userServiceBaseUrl + "/";
    }

    /**
     * Returns the current user (internal id, username, etc.) by forwarding the Bearer token.
     * Returns empty if userservice returns 404 (user not registered) or 401.
     */
    public Optional<UserResponse> getCurrentUser(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    userServiceBaseUrl + "api/v1/users/me",
                    HttpMethod.GET,
                    entity,
                    UserResponse.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}

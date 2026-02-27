package com.dhruvsharma.feed.userservice.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dhruvsharma.feed.userservice.dto.UserResponse;
import com.dhruvsharma.feed.userservice.dto.UserSignupRequest;
import com.dhruvsharma.feed.userservice.dto.UserSignupResponse;
import com.dhruvsharma.feed.userservice.repository.UserRepository;
import com.dhruvsharma.feed.userservice.repository.entity.User;
import com.dhruvsharma.feed.userservice.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserSignupResponse signup(UserSignupRequest userSignupRequest) {
        var existing = userRepository.findByKeycloakSubId(userSignupRequest.getKeycloakSubId());
        if (existing.isPresent()) {
            User u = existing.get();
            return new UserSignupResponse(u.getKeycloakSubId(), u.getUsername(), false);
        }
        User user = new User();
        user.setKeycloakSubId(userSignupRequest.getKeycloakSubId());
        user.setUsername(userSignupRequest.getUsername() != null ? userSignupRequest.getUsername() : userSignupRequest.getKeycloakSubId());
        user.setEmail(userSignupRequest.getEmail());
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);
        return new UserSignupResponse(user.getKeycloakSubId(), user.getUsername(), true);
    }

    @Override
    public Optional<UserResponse> findByKeycloakSubId(String keycloakSubId) {
        return userRepository.findByKeycloakSubId(keycloakSubId)
                .map(u -> new UserResponse(u.getId(), u.getKeycloakSubId(), u.getUsername(), u.getEmail()));
    }

    @Override
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id)
                .map(u -> new UserResponse(u.getId(), u.getKeycloakSubId(), u.getUsername(), u.getEmail()));
    }
}

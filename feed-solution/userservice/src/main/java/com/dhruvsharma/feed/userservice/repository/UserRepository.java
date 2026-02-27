package com.dhruvsharma.feed.userservice.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.dhruvsharma.feed.userservice.repository.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByKeycloakSubId(String keycloakSubId);
}

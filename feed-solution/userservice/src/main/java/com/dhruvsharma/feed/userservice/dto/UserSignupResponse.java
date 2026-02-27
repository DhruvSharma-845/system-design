package com.dhruvsharma.feed.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupResponse {
    private String keycloakSubId;
    private String username;
    private boolean created;
}

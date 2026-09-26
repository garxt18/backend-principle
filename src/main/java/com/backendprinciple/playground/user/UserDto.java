package com.backendprinciple.playground.user;

import java.time.Instant;
import java.util.UUID;

/** What the API exposes about a user. The entity (with its password hash) never leaves the service layer. */
public record UserDto(UUID id, String email, String displayName, Role role,
                      PreferredLanguage preferredLanguage, boolean enabled, Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(),
                u.getPreferredLanguage(), u.isEnabled(), u.getCreatedAt());
    }
}

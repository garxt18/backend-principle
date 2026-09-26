package com.backendprinciple.playground.common.security;

import com.backendprinciple.playground.user.Role;
import java.util.UUID;

/** The authenticated caller, built from the verified JWT claims. */
public record AuthUser(UUID id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

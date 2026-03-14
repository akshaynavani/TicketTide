package com.tickettide.common.security;

import java.util.UUID;

/**
 * Principal reconstructed from a verified JWT. Services never call auth-service
 * to resolve the caller; everything they need lives in the token claims.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean hasRole(Role required) {
        return role == required || role == Role.ADMIN;
    }
}

package com.tickettide.auth.api;

import com.tickettide.auth.user.User;
import com.tickettide.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** Self-registration only allows ATTENDEE or ORGANIZER; ADMIN is granted by another admin. */
    public enum SignupRole { ATTENDEE, ORGANIZER }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String fullName,
            SignupRole role) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    public record ChangeRoleRequest(@NotNull Role role) {
    }

    public record UserResponse(UUID id, String email, String fullName, Role role, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getCreatedAt());
        }
    }
}

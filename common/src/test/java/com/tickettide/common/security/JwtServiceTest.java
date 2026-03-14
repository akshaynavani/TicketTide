package com.tickettide.common.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";
    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "a@b.com", Role.ORGANIZER);

    @Test
    void roundTripsClaims() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));
        String token = jwt.issue(user);
        assertThat(jwt.verify(token)).contains(user);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = new JwtService(SECRET, Duration.ofHours(1)).issue(user);
        JwtService other = new JwtService("another-secret-that-is-also-32-bytes-long", Duration.ofHours(1));
        assertThat(other.verify(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        Instant issuedAt = Instant.parse("2024-01-01T00:00:00Z");
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(5), Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.issue(user);
        JwtService later = new JwtService(SECRET, Duration.ofMinutes(5),
                Clock.fixed(issuedAt.plus(Duration.ofMinutes(10)), ZoneOffset.UTC));
        assertThat(later.verify(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(new JwtService(SECRET, Duration.ofHours(1)).verify("not.a.jwt")).isEmpty();
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new JwtService("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}

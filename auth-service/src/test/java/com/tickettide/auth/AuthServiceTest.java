package com.tickettide.auth;

import com.tickettide.auth.api.AuthDtos.LoginRequest;
import com.tickettide.auth.api.AuthDtos.RegisterRequest;
import com.tickettide.auth.api.AuthDtos.SignupRole;
import com.tickettide.auth.api.AuthDtos.TokenResponse;
import com.tickettide.auth.user.AuthService;
import com.tickettide.auth.user.User;
import com.tickettide.auth.user.UserRepository;
import com.tickettide.common.security.JwtService;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("unit-test-secret-unit-test-secret-1234", Duration.ofHours(1));
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, jwt);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registerNormalizesEmailAndDefaultsToAttendee() {
        TokenResponse res = service.register(new RegisterRequest("  Jane@Example.COM ", "password123", "Jane", null));

        assertThat(res.user().email()).isEqualTo("jane@example.com");
        assertThat(res.user().role()).isEqualTo(Role.ATTENDEE);
        assertThat(jwt.verify(res.accessToken())).hasValueSatisfying(p -> {
            assertThat(p.email()).isEqualTo("jane@example.com");
            assertThat(p.role()).isEqualTo(Role.ATTENDEE);
        });
    }

    @Test
    void registerAllowsOrganizerSignup() {
        TokenResponse res = service.register(new RegisterRequest("org@x.io", "password123", "Org", SignupRole.ORGANIZER));
        assertThat(res.user().role()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("dup@x.io")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("dup@x.io", "password123", "Dup", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginRejectsWrongPassword() {
        User stored = new User("a@x.io", encoder.encode("correct-horse"), "A", Role.ATTENDEE);
        when(users.findByEmailIgnoreCase("a@x.io")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.login(new LoginRequest("a@x.io", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        User stored = new User("a@x.io", encoder.encode("correct-horse"), "A", Role.ORGANIZER);
        when(users.findByEmailIgnoreCase("a@x.io")).thenReturn(Optional.of(stored));

        TokenResponse res = service.login(new LoginRequest("a@x.io", "correct-horse"));
        assertThat(jwt.verify(res.accessToken())).contains(stored.toPrincipal());
        assertThat(res.expiresInSeconds()).isEqualTo(3600);
    }
}

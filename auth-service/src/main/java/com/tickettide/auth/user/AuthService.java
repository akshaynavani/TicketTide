package com.tickettide.auth.user;

import com.tickettide.auth.api.AuthDtos.LoginRequest;
import com.tickettide.auth.api.AuthDtos.RegisterRequest;
import com.tickettide.auth.api.AuthDtos.TokenResponse;
import com.tickettide.auth.api.AuthDtos.UserResponse;
import com.tickettide.common.security.JwtService;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        Role role = req.role() == null ? Role.ATTENDEE : Role.valueOf(req.role().name());
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.fullName().trim(), role));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(normalize(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return users.findById(id).map(UserResponse::from).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional
    public UserResponse changeRole(UUID id, Role role) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        user.changeRole(role);
        return UserResponse.from(user);
    }

    private TokenResponse tokenFor(User user) {
        String token = jwtService.issue(user.toPrincipal());
        return new TokenResponse(token, "Bearer", jwtService.ttl().toSeconds(), UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

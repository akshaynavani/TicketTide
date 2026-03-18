package com.tickettide.auth.api;

import com.tickettide.auth.api.AuthDtos.ChangeRoleRequest;
import com.tickettide.auth.api.AuthDtos.UserResponse;
import com.tickettide.auth.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return authService.get(id);
    }

    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return authService.changeRole(id, request.role());
    }
}

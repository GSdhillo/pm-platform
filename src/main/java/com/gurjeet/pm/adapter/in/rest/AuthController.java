package com.gurjeet.pm.adapter.in.rest;

import com.gurjeet.pm.adapter.in.rest.dto.AuthDtos.*;
import com.gurjeet.pm.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        var result = authService.signup(request.email(), request.displayName(), request.password());
        return new AuthResponse(result.token(), result.user().getId().toString(),
                result.user().getEmail(), result.user().getDisplayName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.email(), request.password());
        return new AuthResponse(result.token(), result.user().getId().toString(),
                result.user().getEmail(), result.user().getDisplayName());
    }
}

package com.gurjeet.pm.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(@NotBlank @Email String email,
                                @NotBlank @Size(max = 100) String displayName,
                                @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, String userId, String email, String displayName) {}
}

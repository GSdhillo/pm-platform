package com.gurjeet.pm.application;

import com.gurjeet.pm.adapter.out.persistence.UserRepository;
import com.gurjeet.pm.common.error.BadRequestException;
import com.gurjeet.pm.common.error.ForbiddenException;
import com.gurjeet.pm.common.security.JwtService;
import com.gurjeet.pm.domain.model.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record AuthResult(String token, UserEntity user) {}

    @Transactional
    public AuthResult signup(String email, String displayName, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        UserEntity user = new UserEntity(email, displayName, passwordEncoder.encode(password));
        userRepository.save(user);
        return new AuthResult(jwtService.issue(user.getId(), email, displayName), user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ForbiddenException("Invalid email or password");
        }
        return new AuthResult(jwtService.issue(user.getId(), user.getEmail(), user.getDisplayName()), user);
    }
}

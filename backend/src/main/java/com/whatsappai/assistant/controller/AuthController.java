package com.whatsappai.assistant.controller;

import com.whatsappai.assistant.dto.LoginRequest;
import com.whatsappai.assistant.dto.LoginResponse;
import com.whatsappai.assistant.entity.AdminUser;
import com.whatsappai.assistant.repository.AdminUserRepository;
import com.whatsappai.assistant.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return adminUserRepository.findByEmail(req.getEmail())
                .filter(u -> passwordEncoder.matches(req.getPassword(), u.getPasswordHash()))
                .map(u -> ResponseEntity.ok((Object) new LoginResponse(
                        jwtService.generateToken(u.getEmail(), u.getBusinessId()),
                        u.getBusinessId(),
                        u.getEmail())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid email or password."));
    }
}

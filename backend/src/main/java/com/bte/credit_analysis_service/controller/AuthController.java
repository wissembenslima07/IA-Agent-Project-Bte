package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.AuthResponse;
import com.bte.credit_analysis_service.dto.LoginRequest;
import com.bte.credit_analysis_service.dto.UtilisateurResponse;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> me(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(new UtilisateurResponse(
            utilisateur.getId(),
            utilisateur.getNom(),
            utilisateur.getPrenom(),
            utilisateur.getEmail(),
            utilisateur.getRole().name()
        ));
    }
}
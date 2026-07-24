package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.AuthResponse;
import com.bte.credit_analysis_service.dto.LoginRequest;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.UtilisateurRepository;
import com.bte.credit_analysis_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Utilisateur user = utilisateurRepository.findByEmail(request.email())
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getRole().name(), user.getEmail());
    }
}
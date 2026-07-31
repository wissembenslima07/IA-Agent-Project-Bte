package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.AuthResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bte.credit_analysis_service.dto.LoginRequest;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.UtilisateurRepository;
import com.bte.credit_analysis_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse authenticate(LoginRequest request) {

        Utilisateur user = utilisateurRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        boolean match = passwordEncoder.matches(
                request.password(),
                user.getMotDePasse()
        );

        System.out.println("Password match = " + match);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getRole().name(),
                user.getEmail()
        );
    }
}
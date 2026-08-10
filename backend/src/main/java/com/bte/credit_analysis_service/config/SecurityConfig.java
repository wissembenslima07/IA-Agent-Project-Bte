package com.bte.credit_analysis_service.config;

import com.bte.credit_analysis_service.security.CustomUserDetailsService;
import com.bte.credit_analysis_service.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configuration de la chaîne de filtrage de sécurité
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Désactiver CSRF pour API REST JWT
            .csrf(csrf -> csrf.disable())

            // Pas de session serveur (stateless) avec JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Utiliser notre authentication provider avec BCrypt
            .authenticationProvider(authenticationProvider())

            // ============ CONFIGURATION DES AUTORISATIONS ============
            .authorizeHttpRequests(auth -> auth
                // 1️⃣ ENDPOINTS PUBLICS (sans authentification)
                
                // Actuator health (pour monitoring)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                
                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Auth: login (public)
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                
                // 2️⃣ ENDPOINTS AUTHENTIFIÉS
                
                // Auth: info utilisateur connecté (requiert token)
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                
                // Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Dossiers de crédit
                .requestMatchers(HttpMethod.POST, "/api/dossiers").hasAnyRole("CONSEILLER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/dossiers").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/dossiers/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/dossiers/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/dossiers/**").authenticated()
                
                // Documents (Sprint 4)
                .requestMatchers("/api/dossiers/*/documents/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/documents/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/documents/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/documents/**").authenticated()
                
                // Analyse IA (Sprint 5)
                .requestMatchers("/api/analyse/**").authenticated()
                
                // OCR/Extraction (Sprint 6)
                .requestMatchers("/api/ocr/**").authenticated()
                
                // 3️⃣ TOUT LE RESTE → Authentifié par défaut
                .anyRequest().authenticated()
            )

            // ============ FILTRE JWT ============
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées
        configuration.setAllowedOrigins(List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:8080",
            "http://localhost:8000"
        ));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(List.of("*"));

        // Autoriser credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Cache de la config CORS (1 heure)
        configuration.setMaxAge(3600L);

        // Appliquer à tous les endpoints
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Authentication provider avec BCrypt
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Encodeur de mot de passe BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * WebClient pour appels aux services externes (FastAPI, etc)
     */
    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
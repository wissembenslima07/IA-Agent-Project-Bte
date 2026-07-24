package com.bte.credit_analysis_service.security;

import com.bte.credit_analysis_service.model.Role;
import com.bte.credit_analysis_service.model.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void tokenGenereEtValideCorrectement() {
        Utilisateur user = new Utilisateur();
        user.setEmail("test@bte.tn");
        user.setRole(Role.CONSEILLER);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("test@bte.tn", jwtService.extractEmail(token));
    }
}
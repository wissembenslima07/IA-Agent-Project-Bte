package com.bte.credit_analysis_service.security;

import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PasswordMatchDiagnosticTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Test
    void verifierCorrespondanceMotDePasse() {
        Utilisateur user = utilisateurRepository.findByEmail("wissem.benslima@bte.tn")
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        String hashEnBase = user.getPassword();
        System.out.println("Hash stocke en base : " + hashEnBase);

        boolean matches = passwordEncoder.matches("admin", hashEnBase);
        System.out.println("Correspondance mot de passe : " + matches);

        assertTrue(matches, "Le mot de passe en clair ne correspond PAS au hash stocke en base");
    }

    @Test
void genererNouveauHash() {
    String hash = passwordEncoder.encode("admin");
    System.out.println("NOUVEAU HASH A COPIER : " + hash);
}
}
package com.bte.credit_analysis_service.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "admin";

        String hash = encoder.encode(password);

        System.out.println("Mot de passe : " + password);
        System.out.println("Hash BCrypt :");
        System.out.println(hash);

        System.out.println();
        System.out.println("Vérification : " + encoder.matches(password, hash));
    }
}
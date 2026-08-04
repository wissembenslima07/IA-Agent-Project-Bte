package com.bte.credit_analysis_service.dto;

public record UtilisateurResponse(
    Long id,
    String nom,
    String prenom,
    String email,
    String role
) {}
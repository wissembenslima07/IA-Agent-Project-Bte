package com.bte.credit_analysis_service.dto;

import com.bte.credit_analysis_service.model.StatutDossier;
import java.time.LocalDateTime;

public record DossierResponse(
    Long id,
    String clientNom,
    String clientPrenom,
    String conseillerEmail,
    StatutDossier statut,
    LocalDateTime dateCreation
) {}
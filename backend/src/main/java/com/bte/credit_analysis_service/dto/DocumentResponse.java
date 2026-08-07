package com.bte.credit_analysis_service.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
    Long id,
    String typeDocument,
    String nomFichier,
    Long tailleBytes,
    String mimeType,
    LocalDateTime dateUpload,
    String uploadePar,
    String statut
) {}
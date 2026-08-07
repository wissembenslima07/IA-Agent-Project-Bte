package com.bte.credit_analysis_service.dto;

import java.time.LocalDateTime;

public record DonneesExtraites(
    Long documentId,
    String textComplet,
    String methode,
    Double confidenceMoyenne,
    Integer nombrePages,
    Integer nombreElements,
    LocalDateTime dateExtraction
) {}
package com.bte.credit_analysis_service.dto;

import java.time.LocalDateTime;

public record HistoriqueActionResponse(
    Long id,
    String action,
    String details,
    String utilisateurEmail,
    LocalDateTime dateAction
) {}
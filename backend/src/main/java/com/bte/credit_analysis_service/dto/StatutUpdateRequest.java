package com.bte.credit_analysis_service.dto;

import com.bte.credit_analysis_service.model.StatutDossier;
import jakarta.validation.constraints.NotNull;

public record StatutUpdateRequest(@NotNull StatutDossier statut) {}
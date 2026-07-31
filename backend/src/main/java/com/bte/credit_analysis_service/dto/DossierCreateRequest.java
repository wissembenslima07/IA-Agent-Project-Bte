package com.bte.credit_analysis_service.dto;

import jakarta.validation.constraints.NotBlank;

public record DossierCreateRequest(
    @NotBlank String clientNom,
    @NotBlank String clientPrenom
) {}
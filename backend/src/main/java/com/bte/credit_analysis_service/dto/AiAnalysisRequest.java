package com.bte.credit_analysis_service.dto;

import java.util.List;

public record AiAnalysisRequest(
    Long dossierId,
    String clientNom,
    String clientPrenom,
    List<DocumentForAi> documents,
    String contexteSupplementaire
) {}

record DocumentForAi(String typeDocument, String contenu) {}
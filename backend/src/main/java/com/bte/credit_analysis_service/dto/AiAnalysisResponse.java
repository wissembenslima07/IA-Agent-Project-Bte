package com.bte.credit_analysis_service.dto;

import java.util.List;

public record AiAnalysisResponse(
    Long dossierId,
    int score_risque,
    String verdict,
    String justification,
    List<String> recommandations
) {}
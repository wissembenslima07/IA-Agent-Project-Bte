package com.bte.credit_analysis_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Input pour POST /api/analyse/evaluate-multiple-fiches
 * Format: JSON avec texte déjà extrait (OCR fait)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateMultipleFichesRequest {
    
    // Identification du dossier
    private Long dossierId;
    private String clientName;
    private String clientEmail;
    
    // Les fiches (texte déjà extrait)
    private List<FichePayeDTO> fiches;
    
    /**
     * Une fiche de paie extraite
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FichePayeDTO {
        private String mois;              // "Janvier 2024"
        private String texteExtrait;      // Texte OCR complet de la fiche
    }
}
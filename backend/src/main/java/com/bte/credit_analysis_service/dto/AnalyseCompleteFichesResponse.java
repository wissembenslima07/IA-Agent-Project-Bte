package com.bte.credit_analysis_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Output complet du pipeline Sprint 7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseCompleteFichesResponse {
    
    private Long verdictId;
    private Long dossierId;
    private Integer nombreFichesAnalysees;
    private String periode;
    private LocalDateTime timestamp;
    
    // Verdict final
    private Integer scoreRisque;
    private String verdict;              // VALIDE|RISQUE|REJETE
    private Double confiance;
    
    // Détails
    private List<String> pointsForts;
    private List<String> risquesMajeurs;
    private List<String> tendancesObservees;
    
    // Recommandations
    private BigDecimal montantMaxRecommande;
    private String dureeMaxRecommandee;
    private List<String> conditionsSpeciales;
    private String tauxInteretRecommande;
    
    // Justification
    private String justification;
    private String resumeCourt;
    
    // Fiches détaillées
    private List<FicheAnalyseeDTO> fichesAnalysees;
    
    /**
     * DTO pour chaque fiche analysée
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FicheAnalyseeDTO {
        private Long id;
        private String mois;
        private BigDecimal salaireBrut;
        private BigDecimal salaireNet;
        private BigDecimal revenus_fiables;
        private BigDecimal revenusComplementaires;
        private String stabiliteSalaire;
    }
}
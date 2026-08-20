package com.bte.credit_analysis_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "verdicts_credit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerdictCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dossier_id", nullable = false)
    private Long dossierId;

    @Column(name = "nombreFichesAnalysees", nullable = false)
    private Integer nombreFichesAnalysees;

    @Column(name = "periode", nullable = false)
    private String periode;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "scoreRisque", nullable = false)
    private Integer scoreRisque;

    @Column(name = "verdict", nullable = false)
    private String verdict;

    @Column(name = "confiance", nullable = false)
    private Double confiance;

    @Column(name = "pointsForts", columnDefinition = "TEXT")
    private String pointsForts;

    @Column(name = "risquesMajeurs", columnDefinition = "TEXT")
    private String risquesMajeurs;

    @Column(name = "tendancesObservees", columnDefinition = "TEXT")
    private String tendancesObservees;

    @Column(name = "montantMaxRecommande")
    private BigDecimal montantMaxRecommande;

    @Column(name = "dureeMaxRecommandee")
    private String dureeMaxRecommandee;

    @Column(name = "conditionsSpeciales", columnDefinition = "TEXT")
    private String conditionsSpeciales;

    @Column(name = "tauxInteretRecommande")
    private String tauxInteretRecommande;

    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "resumeCourt", nullable = false)
    private String resumeCourt;

    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(name = "statut")
    private String statut;

    @Column(name = "commentaire_validation", columnDefinition = "TEXT")
    private String commentaireValidation;
}
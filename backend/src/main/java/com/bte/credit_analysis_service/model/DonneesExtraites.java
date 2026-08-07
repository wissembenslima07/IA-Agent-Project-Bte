package com.bte.credit_analysis_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "donnees_extraites")
@Data
@NoArgsConstructor
public class DonneesExtraites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierCredit dossier;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String textComplet;

    @Column(name = "methode")
    private String methode;  // pdf_text, ocr_paddle

    @Column(name = "confidence_moyenne")
    private Double confidenceMoyenne;  // 0.0 à 1.0

    @Column(name = "nombre_pages")
    private Integer nombrePages;

    @Column(name = "nombre_elements")
    private Integer nombreElements;

    @Column(name = "date_extraction")
    private LocalDateTime dateExtraction = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur verifiePar;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutExtraction statut = StatutExtraction.VALIDE;

    public enum StatutExtraction {
        VALIDE,
        EN_REVISION,
        REJETE
    }
}
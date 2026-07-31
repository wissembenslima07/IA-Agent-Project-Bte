package com.bte.credit_analysis_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dossiers_credit")
@Data
@NoArgsConstructor
public class DossierCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_nom", nullable = false)
    private String clientNom;

    @Column(name = "client_prenom", nullable = false)
    private String clientPrenom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conseiller_id")
    private Utilisateur conseiller;

    @Enumerated(EnumType.STRING)
    private StatutDossier statut = StatutDossier.EN_COURS;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();
}
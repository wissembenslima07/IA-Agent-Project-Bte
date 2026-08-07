package com.bte.credit_analysis_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierCredit dossier;

    @Column(name = "type_document", nullable = false)
    private String typeDocument; // ex: "FICHE_PAIE", "RELEVE_BANCAIRE", "PIECE_IDENTITE"

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier; // nom original du fichier

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier; // chemin/URI unique genere par StorageService

    @Column(name = "taille_bytes")
    private Long tailleBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "date_upload")
    private LocalDateTime dateUpload = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur uploadePar; // qui a upload le document

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutDocument statut = StatutDocument.EN_ATTENTE;

    public enum StatutDocument {
        EN_ATTENTE,
        ANALYSE,
        VALIDE,
        REJETE
    }
}
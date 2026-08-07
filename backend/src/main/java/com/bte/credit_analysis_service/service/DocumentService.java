package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.DocumentResponse;
import com.bte.credit_analysis_service.model.Document;
import com.bte.credit_analysis_service.model.DossierCredit;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.DocumentRepository;
import com.bte.credit_analysis_service.repository.DossierCreditRepository;
import com.bte.credit_analysis_service.service.storage.StorageService;
import com.bte.credit_analysis_service.util.FileValidationUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DossierCreditRepository dossierCreditRepository;
    private final StorageService storageService;
    private final HistoriqueActionService historiqueActionService;

    public DocumentResponse uploadDocument(Long dossierId, String typeDocument, MultipartFile file, Utilisateur utilisateur) {
        // Valider le fichier
        FileValidationUtil.validateFile(file);

        // Verifier que le dossier existe
        DossierCredit dossier = dossierCreditRepository.findById(dossierId)
            .orElseThrow(() -> new EntityNotFoundException("Dossier introuvable: " + dossierId));

        // Stocker le fichier
        String bucketName = "dossier_" + dossierId; // Organiser par dossier
        String fileName = file.getOriginalFilename();
        String cheminFichier = storageService.store(bucketName, fileName, file);

        // Creer l'entite Document
        Document doc = new Document();
        doc.setDossier(dossier);
        doc.setTypeDocument(typeDocument);
        doc.setNomFichier(fileName);
        doc.setCheminFichier(cheminFichier);
        doc.setTailleBytes(file.getSize());
        doc.setMimeType(file.getContentType());
        doc.setUploadePar(utilisateur);

        Document saved = documentRepository.save(doc);

        // Journaliser l'action
        historiqueActionService.enregistrer(dossier, utilisateur, "UPLOAD_DOCUMENT",
            "Document '" + fileName + "' (" + typeDocument + ") telecharge");

        log.info("Document {} uploaded pour le dossier {}", fileName, dossierId);

        return toResponse(saved);
    }

    public List<DocumentResponse> listerDocumentsDuDossier(Long dossierId) {
        // Verifier que le dossier existe
        dossierCreditRepository.findById(dossierId)
            .orElseThrow(() -> new EntityNotFoundException("Dossier introuvable: " + dossierId));

        return documentRepository.findByDossierId(dossierId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ResponseEntity<Resource> consulterDocument(Long dossierId, Long documentId) {
        Document doc = documentRepository.findByIdAndDossierId(documentId, dossierId)
            .orElseThrow(() -> new EntityNotFoundException("Document introuvable: " + documentId));

        String bucketName = "dossier_" + dossierId;
        InputStream documentStream = storageService.retrieve(bucketName, doc.getCheminFichier());

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (doc.getMimeType() != null && !doc.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(doc.getMimeType());
            } catch (IllegalArgumentException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline()
                    .filename(doc.getNomFichier())
                    .build()
                    .toString())
            .contentType(mediaType)
            .body(new InputStreamResource(documentStream));
    }

    public void supprimerDocument(Long documentId, Utilisateur utilisateur) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new EntityNotFoundException("Document introuvable: " + documentId));

        // Supprimer le fichier du stockage
        String bucketName = "dossier_" + doc.getDossier().getId();
        storageService.delete(bucketName, doc.getCheminFichier());

        // Supprimer la metadata de la base
        documentRepository.deleteById(documentId);

        // Journaliser
        historiqueActionService.enregistrer(doc.getDossier(), utilisateur, "SUPPRESSION_DOCUMENT",
            "Document '" + doc.getNomFichier() + "' supprime");

        log.info("Document {} supprime", documentId);
    }

    private DocumentResponse toResponse(Document doc) {
        return new DocumentResponse(
            doc.getId(),
            doc.getTypeDocument(),
            doc.getNomFichier(),
            doc.getTailleBytes(),
            doc.getMimeType(),
            doc.getDateUpload(),
            doc.getUploadePar() != null ? doc.getUploadePar().getEmail() : "SYSTEME",
            doc.getStatut().name()
        );
    }
}
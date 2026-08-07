package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.DocumentResponse;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.service.DocumentService;
import com.bte.credit_analysis_service.util.FileValidationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dossiers/{dossierId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long dossierId,
            @RequestParam String typeDocument,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        DocumentResponse response = documentService.uploadDocument(dossierId, typeDocument, file, utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<DocumentResponse>> uploadMultipleDocuments(
            @PathVariable Long dossierId,
            @RequestParam MultipartFile[] files,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        // Valider la taille totale
        FileValidationUtil.validateTotalSize(files);

        List<DocumentResponse> responses = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            // Utiliser un type par defaut "DOCUMENT_GENERAL" si non specifie
            DocumentResponse doc = documentService.uploadDocument(dossierId, "DOCUMENT_GENERAL", file, utilisateur);
            responses.add(doc);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listerDocuments(@PathVariable Long dossierId) {
        return ResponseEntity.ok(documentService.listerDocumentsDuDossier(dossierId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> consulterDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentId) {

        return documentService.consulterDocument(dossierId, documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> supprimerDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        documentService.supprimerDocument(documentId, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
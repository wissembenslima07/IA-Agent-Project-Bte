package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.DonneesExtraites;
import com.bte.credit_analysis_service.service.OCRExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.bte.credit_analysis_service.model.Utilisateur;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OCRController {

    private final OCRExtractionService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<DonneesExtraites> extraireDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentId") Long documentId,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        DonneesExtraites donnees = ocrService.extraireDocument(file, documentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(donnees);
    }
}
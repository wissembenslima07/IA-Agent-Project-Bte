package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.*;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.service.DossierCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierCreditController {

    private final DossierCreditService dossierCreditService;

    @PostMapping
    public ResponseEntity<DossierResponse> creer(
            @Valid @RequestBody DossierCreateRequest request,
            @AuthenticationPrincipal Utilisateur conseiller) {
        DossierResponse response = dossierCreditService.creerDossier(request, conseiller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<DossierResponse>> lister(
            @AuthenticationPrincipal Utilisateur conseiller,
            Pageable pageable) {
        return ResponseEntity.ok(
            dossierCreditService.listerDossiersDuConseiller(conseiller.getId(), pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DossierResponse> consulter(@PathVariable Long id) {
        return ResponseEntity.ok(dossierCreditService.consulterDossier(id));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<DossierResponse> changerStatut(
            @PathVariable Long id,
            @Valid @RequestBody StatutUpdateRequest request,
            @AuthenticationPrincipal Utilisateur acteur) {
        return ResponseEntity.ok(dossierCreditService.mettreAJourStatut(id, request, acteur));
    }

    @GetMapping("/{id}/historique")
    public ResponseEntity<List<HistoriqueActionResponse>> historique(@PathVariable Long id) {
        return ResponseEntity.ok(dossierCreditService.consulterHistorique(id));
    }
}
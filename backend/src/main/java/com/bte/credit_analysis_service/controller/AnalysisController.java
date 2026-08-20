package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.EvaluateMultipleFichesRequest;
import com.bte.credit_analysis_service.dto.AnalyseCompleteFichesResponse;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.service.CreditAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analyse")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final CreditAnalysisService creditAnalysisService;

    /**
     * Pipeline Sprint 7: Analyser multiple fiches et sauvegarder tout
     */
    @PostMapping("/evaluate-multiple-fiches")
    public ResponseEntity<?> evaluateMultipleFiches(
            @RequestBody EvaluateMultipleFichesRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        log.info("📋 POST /api/analyse/evaluate-multiple-fiches - Dossier: {}", request.getDossierId());

        try {
            // Valider la requête
            if (request.getDossierId() == null || request.getFiches() == null || request.getFiches().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "dossierId et fiches obligatoires"
                ));
            }

            if (request.getFiches().size() < 2 || request.getFiches().size() > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Entre 2 et 12 fiches requises"
                ));
            }

            // Exécuter le pipeline
            AnalyseCompleteFichesResponse response = creditAnalysisService.analyzeMultipleFiches(request, utilisateur);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "verdict_id", response.getVerdictId(),
                    "dossier_id", response.getDossierId(),
                    "verdict", response.getVerdict(),
                    "score_risque", response.getScoreRisque(),
                    "confiance", response.getConfiance(),
                    "montant_max_recommande", response.getMontantMaxRecommande(),
                    "duree_max_recommandee", response.getDureeMaxRecommandee(),
                    "justification", response.getJustification(),
                    "data", response
            ));

        } catch (Exception e) {
            log.error("❌ Erreur analyse", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Analysis Service UP");
    }

    /**
     * Statistiques globales pour le dashboard (score moyen de risque sur le dernier
     * verdict de chaque dossier analysé).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> statistiques() {
        return ResponseEntity.ok(creditAnalysisService.calculerStatistiquesRisque());
    }
}
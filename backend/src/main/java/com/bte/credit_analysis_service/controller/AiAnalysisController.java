package com.bte.credit_analysis_service.controller;

import com.bte.credit_analysis_service.dto.AiAnalysisRequest;
import com.bte.credit_analysis_service.dto.AiAnalysisResponse;
import com.bte.credit_analysis_service.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyse")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    public ResponseEntity<AiAnalysisResponse> lancerAnalyse(@RequestBody AiAnalysisRequest request) {
        AiAnalysisResponse response = aiAnalysisService.analyserDossier(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        aiAnalysisService.verifierSanteServiceIa();
        return ResponseEntity.ok("Service d'analyse disponible");
    }
}
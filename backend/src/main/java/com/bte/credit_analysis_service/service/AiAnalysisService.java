package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.AiAnalysisRequest;
import com.bte.credit_analysis_service.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class AiAnalysisService {

    private final RestClient restClient;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AiAnalysisService() {
        this.restClient = RestClient.builder().build();
    }

    public AiAnalysisResponse analyserDossier(AiAnalysisRequest request) {
        log.info("Envoi de la demande d'analyse au service IA pour le dossier {}", request.dossierId());

        try {
            AiAnalysisResponse response = restClient.post()
                .uri(aiServiceUrl + "/api/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiAnalysisResponse.class);

            log.info("Analyse complétée pour dossier {}: score={}, verdict={}",
                request.dossierId(), response.score_risque(), response.verdict());

            return response;
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service IA", e);
            throw new RuntimeException("Impossible d'analyser le dossier avec l'IA: " + e.getMessage(), e);
        }
    }

    public void verifierSanteServiceIa() {
        try {
            restClient.get()
                .uri(aiServiceUrl + "/health")
                .retrieve()
                .body(String.class);
            log.info("Service IA vérifiée et disponible");
        } catch (Exception e) {
            log.warn("Service IA indisponible: {}", e.getMessage());
        }
    }
}
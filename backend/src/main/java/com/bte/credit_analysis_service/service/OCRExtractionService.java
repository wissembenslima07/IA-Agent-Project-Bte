package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.DonneesExtraites;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Slf4j
public class OCRExtractionService {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public DonneesExtraites extraireDocument(MultipartFile file, Long documentId) {
        log.info("Extraction OCR du document {}", documentId);

        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("documentId", documentId);

            var response = webClient
                .post()
                .uri(aiServiceUrl + "/api/ocr/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OCRResponseDTO.class)
                .timeout(java.time.Duration.ofSeconds(120))
                .block();

            if (response != null && response.success()) {
                return new DonneesExtraites(
                    documentId,
                    response.donnees().textComplet(),
                    response.donnees().methode(),
                    response.donnees().confidenceMoyenne(),
                    response.donnees().nombrePages(),
                    response.donnees().nombreElements(),
                    LocalDateTime.now()
                );
            }

            throw new RuntimeException("Extraction échouée: " + 
                (response != null ? response.error() : "Réponse vide"));

        } catch (IOException e) {
            log.error("Erreur lecture fichier", e);
            throw new RuntimeException("Erreur lecture fichier: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur extraction OCR", e);
            throw new RuntimeException("Erreur OCR: " + e.getMessage());
        }
    }

    // DTOs internes pour parser la réponse FastAPI
    record OCRResponseDTO(
        boolean success,
        long documentId,
        OCRDonneesDTO donnees,
        String error
    ) {}

    record OCRDonneesDTO(
        long documentId,
        String textComplet,
        String methode,
        double confidenceMoyenne,
        Integer nombrePages,
        Integer nombreElements
    ) {}
}
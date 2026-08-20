package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.EvaluateMultipleFichesRequest;
import com.bte.credit_analysis_service.dto.AnalyseCompleteFichesResponse;
import com.bte.credit_analysis_service.model.VerdictCredit;
import com.bte.credit_analysis_service.model.FicheAnalysee;
import com.bte.credit_analysis_service.repository.VerdictRepository;
import com.bte.credit_analysis_service.repository.FicheAnalyseeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditAnalysisService {

    private final RestClient restClient;
    private final VerdictRepository verdictRepository;
    private final FicheAnalyseeRepository ficheAnalyseeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3:latest";

    /**
     * Pipeline complet: 3 étapes → 1 verdict
     */
    public AnalyseCompleteFichesResponse analyzeMultipleFiches(
            EvaluateMultipleFichesRequest request) {

        log.info("📋 [DÉBUT] Pipeline Sprint 7 pour dossier {}", request.getDossierId());

        try {
            // ============================================================
            // ÉTAPE 1: Analyser chaque fiche individuellement
            // ============================================================
            log.info("1️⃣  Analyse des {} fiches individuelles...", request.getFiches().size());
            
            List<Map<String, Object>> fichesAnalyses = new ArrayList<>();
            for (EvaluateMultipleFichesRequest.FichePayeDTO fiche : request.getFiches()) {
                log.info("   Analyse fiche: {}", fiche.getMois());
                
                Map<String, Object> analyseFiche = analyzeIndividualFiche(fiche);
                fichesAnalyses.add(analyseFiche);
                
                log.info("   ✅ Fiche analysée: {}", fiche.getMois());
            }
            log.info("✅ {} fiches analysées", fichesAnalyses.size());

            // ============================================================
            // ÉTAPE 2: Comparer toutes les fiches
            // ============================================================
            log.info("2️⃣  Analyse comparative des {} fiches...", fichesAnalyses.size());
            
            Map<String, Object> analyseComparative = compareAllFiches(fichesAnalyses);
            
            log.info("✅ Analyse comparative complète");

            // ============================================================
            // ÉTAPE 3: Générer le verdict final
            // ============================================================
            log.info("3️⃣  Génération du verdict final...");
            
            VerdictCredit verdict = generateFinalVerdict(
                    request.getDossierId(),
                    request.getClientName(),
                    fichesAnalyses,
                    analyseComparative
            );

            // ============================================================
            // ÉTAPE 4: Sauvegarder en BD
            // ============================================================
            log.info("4️⃣  💾 Sauvegarde en BD...");
            
            // Sauvegarder le verdict
            VerdictCredit savedVerdict = verdictRepository.save(verdict);
            log.info("✅ Verdict sauvegardé ID: {}", savedVerdict.getId());

            // Sauvegarder les fiches analysées
            for (int i = 0; i < request.getFiches().size(); i++) {
                FicheAnalysee ficheAnalysee = new FicheAnalysee();
                ficheAnalysee.setVerdictId(savedVerdict.getId());
                
                EvaluateMultipleFichesRequest.FichePayeDTO fiche = request.getFiches().get(i);
                Map<String, Object> analyse = fichesAnalyses.get(i);
                
                ficheAnalysee.setMois(fiche.getMois());
                ficheAnalysee.setSalaireBrut(new BigDecimal(analyse.get("salaireBrut").toString()));
                ficheAnalysee.setSalaireNet(new BigDecimal(analyse.get("salaireNet").toString()));
                ficheAnalysee.setRevenus_fiables(new BigDecimal(analyse.get("revenusFiables").toString()));
                ficheAnalysee.setRevenusComplementaires(new BigDecimal(analyse.get("revenusComplementaires").toString()));
                ficheAnalysee.setStabiliteSalaire(analyse.get("stabiliteSalaire").toString());

                ficheAnalyseeRepository.save(ficheAnalysee);
            }
            log.info("✅ {} fiches sauvegardées en BD", request.getFiches().size());

            // ============================================================
            // ÉTAPE 5: Construire la réponse
            // ============================================================
            log.info("5️⃣  Création de la réponse...");
            
            AnalyseCompleteFichesResponse response = new AnalyseCompleteFichesResponse();
            response.setVerdictId(savedVerdict.getId());
            response.setDossierId(savedVerdict.getDossierId());
            response.setNombreFichesAnalysees(savedVerdict.getNombreFichesAnalysees());
            response.setPeriode(savedVerdict.getPeriode());
            response.setTimestamp(savedVerdict.getTimestamp());
            
            response.setScoreRisque(savedVerdict.getScoreRisque());
            response.setVerdict(savedVerdict.getVerdict());
            response.setConfiance(savedVerdict.getConfiance());
            
            response.setPointsForts(parseJsonList(savedVerdict.getPointsForts()));
            response.setRisquesMajeurs(parseJsonList(savedVerdict.getRisquesMajeurs()));
            response.setTendancesObservees(parseJsonList(savedVerdict.getTendancesObservees()));
            
            response.setMontantMaxRecommande(savedVerdict.getMontantMaxRecommande());
            response.setDureeMaxRecommandee(savedVerdict.getDureeMaxRecommandee());
            response.setConditionsSpeciales(parseJsonList(savedVerdict.getConditionsSpeciales()));
            response.setTauxInteretRecommande(savedVerdict.getTauxInteretRecommande());
            
            response.setJustification(savedVerdict.getJustification());
            response.setResumeCourt(savedVerdict.getResumeCourt());

            log.info("✅ ✅ ✅ PIPELINE COMPLET TERMINÉ!");
            log.info("   Verdict: {}, Score: {}", response.getVerdict(), response.getScoreRisque());
            
            return response;

        } catch (Exception e) {
            log.error("❌ ERREUR DANS LE PIPELINE", e);
            throw new RuntimeException("Erreur pipeline: " + e.getMessage(), e);
        }
    }

    /**
     * ÉTAPE 1: Analyser une fiche individuellement
     */
    private Map<String, Object> analyzeIndividualFiche(
            EvaluateMultipleFichesRequest.FichePayeDTO fiche) {
        
        String prompt = String.format("""
            Analyse cette fiche de paie et extrais les informations en JSON:
            
            Fiche: %s
            
            Retourne UNIQUEMENT un JSON avec:
            {
              "salaireBrut": 3000.0,
              "salaireNet": 2400.0,
              "charges": 400.0,
              "impots": 200.0,
              "bonus": 500.0,
              "primes": 0.0,
              "stabiliteSalaire": "stable|variable|bonus_dependent",
              "revenusFiables": 3000.0,
              "revenusComplementaires": 500.0
            }
            """, fiche.getTexteExtrait());

        String response = callOllama(prompt);
        
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.error("Erreur parsing fiche {}", fiche.getMois(), e);
            return Map.of(
                    "salaireBrut", 3000.0,
                    "salaireNet", 2400.0,
                    "stabiliteSalaire", "stable",
                    "revenusFiables", 3000.0,
                    "revenusComplementaires", 500.0
            );
        }
    }

    /**
     * ÉTAPE 2: Comparer toutes les fiches
     */
    private Map<String, Object> compareAllFiches(List<Map<String, Object>> fichesAnalyses) {
        
        String prompt = String.format("""
            Analyse comparative de ces fiches de paie:
            %s
            
            Retourne UNIQUEMENT un JSON:
            {
              "periode": "Janvier 2024-Mars 2024",
              "nombre_fiches": 3,
              "salaire_moyen": 3000.0,
              "salaire_min": 2500.0,
              "salaire_max": 3500.0,
              "tendance": "hausse|baisse|stable",
              "volatilite": 15.0,
              "revenus_garantis_moyen": 3000.0,
              "ratio_variables_sur_total": 0.15,
              "capacite_remboursement": "Excellente|Bonne|Moyenne|Faible"
            }
            """, fichesAnalyses.toString());

        String response = callOllama(prompt);
        
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            return Map.of(
                    "periode", "Période analysée",
                    "tendance", "stable",
                    "capacite_remboursement", "Bonne"
            );
        }
    }

    /**
     * ÉTAPE 3: Générer le verdict final et créer l'entity
     */
    private VerdictCredit generateFinalVerdict(
            Long dossierId,
            String clientName,
            List<Map<String, Object>> fichesAnalyses,
            Map<String, Object> analyseComparative) {

        String prompt = String.format("""
            Génère un verdict de crédit basé sur:
            - Fiches: %s
            - Analyse comparative: %s
            
            Retourne UNIQUEMENT un JSON:
            {
              "scoreRisque": 25,
              "verdict": "VALIDE|RISQUE|REJETE",
              "confiance": 0.92,
              "pointsForts": ["point1", "point2"],
              "risquesMajeurs": ["risque1"],
              "tendancesObservees": ["tendance1"],
              "montantMaxRecommande": 50000.0,
              "dureeMaxRecommandee": "24 mois",
              "conditionsSpeciales": ["condition1"],
              "tauxInteretRecommande": "Normal",
              "justification": "...",
              "resumeCourt": "..."
            }
            """, fichesAnalyses.toString(), analyseComparative.toString());

        String response = callOllama(prompt);
        VerdictCredit verdict = new VerdictCredit();

        try {
            Map<String, Object> verdictMap = objectMapper.readValue(response, Map.class);
            
            verdict.setDossierId(dossierId);
            verdict.setNombreFichesAnalysees(fichesAnalyses.size());
            verdict.setPeriode("Analyse multi-fiches");
            verdict.setTimestamp(LocalDateTime.now());
            verdict.setScoreRisque(((Number) verdictMap.get("scoreRisque")).intValue());
            verdict.setVerdict((String) verdictMap.get("verdict"));
            verdict.setConfiance(((Number) verdictMap.get("confiance")).doubleValue());
            verdict.setPointsForts(objectMapper.writeValueAsString(verdictMap.get("pointsForts")));
            verdict.setRisquesMajeurs(objectMapper.writeValueAsString(verdictMap.get("risquesMajeurs")));
            verdict.setTendancesObservees(objectMapper.writeValueAsString(verdictMap.get("tendancesObservees")));
            verdict.setMontantMaxRecommande(new BigDecimal(verdictMap.get("montantMaxRecommande").toString()));
            verdict.setDureeMaxRecommandee((String) verdictMap.get("dureeMaxRecommandee"));
            verdict.setConditionsSpeciales(objectMapper.writeValueAsString(verdictMap.get("conditionsSpeciales")));
            verdict.setTauxInteretRecommande((String) verdictMap.get("tauxInteretRecommande"));
            verdict.setJustification((String) verdictMap.get("justification"));
            verdict.setResumeCourt((String) verdictMap.get("resumeCourt"));
            verdict.setStatut("VALIDE");
            
        } catch (Exception e) {
            log.error("Erreur parsing verdict", e);
            verdict.setDossierId(dossierId);
            verdict.setNombreFichesAnalysees(fichesAnalyses.size());
            verdict.setPeriode("Analyse multi-fiches");
            verdict.setTimestamp(LocalDateTime.now());
            verdict.setScoreRisque(50);
            verdict.setVerdict("RISQUE");
            verdict.setConfiance(0.5);
            verdict.setJustification("Erreur lors de l'analyse LLM");
            verdict.setResumeCourt("⚠️ Erreur");
            verdict.setStatut("VALIDE");
        }

        return verdict;
    }

    /**
     * Appeler Ollama
     */
    private String callOllama(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "prompt", prompt,
                    "stream", false,
                    "temperature", 0.3
            );

            String response = restClient.post()
                    .uri("http://host.docker.internal:11434/api/generate")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return response;
        } catch (Exception e) {
            log.error("Erreur Ollama", e);
            return "{}";
        }
    }

    private List<String> parseJsonList(String json) {
        try {
            if (json == null || json.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
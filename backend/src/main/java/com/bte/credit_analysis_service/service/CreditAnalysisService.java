package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.EvaluateMultipleFichesRequest;
import com.bte.credit_analysis_service.dto.AnalyseCompleteFichesResponse;
import com.bte.credit_analysis_service.model.DossierCredit;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.model.VerdictCredit;
import com.bte.credit_analysis_service.model.FicheAnalysee;
import com.bte.credit_analysis_service.repository.DossierCreditRepository;
import com.bte.credit_analysis_service.repository.VerdictRepository;
import com.bte.credit_analysis_service.repository.FicheAnalyseeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditAnalysisService {

    private final RestClient restClient;
    private final VerdictRepository verdictRepository;
    private final FicheAnalyseeRepository ficheAnalyseeRepository;
    private final DossierCreditRepository dossierCreditRepository;
    private final HistoriqueActionService historiqueActionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MODEL = "llama3:latest";
    private static final String OLLAMA_GENERATE_URL = "http://host.docker.internal:11434/api/generate";

    /**
     * Pipeline en UN SEUL appel LLM: fiches -> verdict complet.
     * (Auparavant 3+ appels séquentiels: analyse par fiche, comparaison, verdict -
     * bien trop lent en inférence CPU locale, remplacé par un appel unique.)
     */
    public AnalyseCompleteFichesResponse analyzeMultipleFiches(
            EvaluateMultipleFichesRequest request, Utilisateur utilisateur) {

        log.info("📋 [DÉBUT] Analyse dossier {} ({} fiches)", request.getDossierId(), request.getFiches().size());

        try {
            DossierCredit dossier = dossierCreditRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new EntityNotFoundException("Dossier introuvable: " + request.getDossierId()));

            Map<String, Object> resultat = analyserToutesLesFiches(request.getFiches());

            List<EvaluateMultipleFichesRequest.FichePayeDTO> fiches = request.getFiches();
            String periode = fiches.get(0).getMois() + "-" + fiches.get(fiches.size() - 1).getMois();

            VerdictCredit verdict = construireVerdict(request.getDossierId(), periode, fiches.size(), resultat);
            verdict.setUtilisateurId(utilisateur != null ? utilisateur.getId() : null);
            // Le montant recommandé par le LLM est une estimation; on le remplace par un calcul
            // déterministe suivant la formule métier, plus fiable qu'une arithmétique générée par le modèle.
            verdict.setMontantMaxRecommande(
                    calculerMontantMaxRecommande(resultat, verdict.getScoreRisque(), verdict.getDureeMaxRecommandee()));
            VerdictCredit savedVerdict = verdictRepository.save(verdict);
            log.info("✅ Verdict sauvegardé ID: {}", savedVerdict.getId());

            sauvegarderFichesAnalysees(savedVerdict.getId(), fiches, resultat);
            log.info("✅ {} fiches sauvegardées en BD", fiches.size());

            historiqueActionService.enregistrer(dossier, utilisateur, "ANALYSE_IA",
                    String.format("Analyse IA effectuée: verdict %s, score de risque %d/100 (%d fiches)",
                            savedVerdict.getVerdict(), savedVerdict.getScoreRisque(), fiches.size()));

            AnalyseCompleteFichesResponse response = construireReponse(savedVerdict);
            log.info("✅ ANALYSE TERMINÉE - Verdict: {}, Score: {}", response.getVerdict(), response.getScoreRisque());

            return response;

        } catch (Exception e) {
            log.error("❌ ERREUR DANS LE PIPELINE", e);
            throw new RuntimeException("Erreur pipeline: " + e.getMessage(), e);
        }
    }

    /**
     * Appel LLM unique: extraction par fiche + verdict final dans la même réponse.
     */
    private Map<String, Object> analyserToutesLesFiches(List<EvaluateMultipleFichesRequest.FichePayeDTO> fiches) {

        StringBuilder fichesTexte = new StringBuilder();
        for (EvaluateMultipleFichesRequest.FichePayeDTO fiche : fiches) {
            fichesTexte.append("--- Fiche: ").append(fiche.getMois()).append(" ---\n")
                    .append(fiche.getTexteExtrait()).append("\n\n");
        }

        String prompt = String.format("""
            Tu es un analyste crédit bancaire. Voici %d fiches de paie réelles à analyser:

            %s

            Calcule chaque valeur ci-dessous en te basant UNIQUEMENT sur les montants et informations lus dans les fiches ci-dessus. N'invente rien et ne réutilise aucune valeur numérique déjà présente dans ce message en dehors des fiches: chaque chiffre que tu écris doit venir d'un calcul ou d'une lecture directe des fiches.

            Réponds UNIQUEMENT avec un JSON valide (aucun texte avant/après), respectant strictement cette structure. Le tableau "fiches" doit contenir EXACTEMENT %d éléments, dans le MÊME ORDRE que les fiches ci-dessus:
            {
              "fiches": [
                {
                  "salaireBrut": <nombre: salaire brut lu sur la fiche>,
                  "salaireNet": <nombre: salaire net lu sur la fiche>,
                  "charges": <nombre: charges/cotisations lues sur la fiche, 0 si absentes>,
                  "impots": <nombre: impôts lus sur la fiche, 0 si absents>,
                  "bonus": <nombre: bonus lu sur la fiche, 0 si absent>,
                  "primes": <nombre: primes lues sur la fiche, 0 si absentes>,
                  "stabiliteSalaire": "<stable, variable ou bonus_dependent selon la fiche>",
                  "revenusFiables": <nombre: partie garantie/fixe du revenu>,
                  "revenusComplementaires": <nombre: partie variable du revenu (bonus+primes)>
                }
              ],
              "scoreRisque": <entier 0-100, calculé à partir des fiches ci-dessus: 0=risque nul, 100=risque maximal>,
              "verdict": "VALIDE, RISQUE ou REJETE selon ton analyse",
              "confiance": <nombre décimal 0.0-1.0: ton niveau de confiance dans ce verdict>,
              "pointsForts": [<0 à 4 chaînes, chacune un point fort concret basé sur les fiches. Tableau vide [] si aucun>],
              "risquesMajeurs": [<0 à 4 chaînes, chacune un risque concret basé sur les fiches. Tableau vide [] si aucun risque - n'écris jamais "aucun" comme élément de la liste>],
              "tendancesObservees": [<0 à 4 chaînes, chacune une tendance observée entre les fiches. Tableau vide [] si non applicable - n'écris jamais "aucune" comme élément de la liste>],
              "montantMaxRecommande": <nombre: estimation du montant de crédit recommandé en euros, en appliquant cette formule:
                MontantCreditMax = [(SalaireNet x TauxEndettementMax - ChargesExistantes) x (1 - RisqueImpayement)] x DureeEnMois
                où TauxEndettementMax = 0.33 (norme bancaire standard), ChargesExistantes = les charges lues sur les fiches,
                RisqueImpayement = scoreRisque / 100, et DureeEnMois = la durée que tu recommandes ci-dessous.
                Cette valeur sera recalculée et vérifiée par le système avec cette même formule - donne une estimation cohérente avec elle>,
              "dureeMaxRecommandee": <chaîne au format "<N> mois" où N est un nombre entier que tu calcules toi-même selon le profil, par exemple "12 mois" ou "36 mois" - N doit être un vrai nombre, jamais la lettre X>,
              "conditionsSpeciales": [<0 à 3 chaînes, chacune une condition recommandée. Tableau vide [] si aucune>],
              "tauxInteretRecommande": "<Normal, Préférentiel ou Majoré selon le profil>",
              "justification": "<2 à 3 phrases expliquant le verdict en citant les vrais chiffres des fiches ci-dessus>",
              "resumeCourt": "<une phrase résumant le profil, basée sur les vraies fiches>"
            }
            """, fiches.size(), fichesTexte, fiches.size());

        String response = callOllama(prompt);

        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Réponse LLM invalide (JSON non parsable): " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private VerdictCredit construireVerdict(Long dossierId, String periode, int nombreFiches, Map<String, Object> r) {
        VerdictCredit verdict = new VerdictCredit();
        verdict.setDossierId(dossierId);
        verdict.setNombreFichesAnalysees(nombreFiches);
        verdict.setPeriode(periode);
        verdict.setTimestamp(LocalDateTime.now());

        try {
            verdict.setScoreRisque(((Number) r.get("scoreRisque")).intValue());
            verdict.setVerdict((String) r.get("verdict"));
            verdict.setConfiance(((Number) r.get("confiance")).doubleValue());
            verdict.setPointsForts(toJson(r.get("pointsForts")));
            verdict.setRisquesMajeurs(toJson(r.get("risquesMajeurs")));
            verdict.setTendancesObservees(toJson(r.get("tendancesObservees")));
            verdict.setMontantMaxRecommande(toBigDecimal(r.get("montantMaxRecommande"), null));
            verdict.setDureeMaxRecommandee((String) r.get("dureeMaxRecommandee"));
            verdict.setConditionsSpeciales(toJson(r.get("conditionsSpeciales")));
            verdict.setTauxInteretRecommande((String) r.get("tauxInteretRecommande"));
            verdict.setJustification((String) r.get("justification"));
            verdict.setResumeCourt((String) r.get("resumeCourt"));
            verdict.setStatut("VALIDE");
        } catch (Exception e) {
            throw new RuntimeException("Réponse LLM incomplète (champ de verdict manquant): " + e.getMessage(), e);
        }

        return verdict;
    }

    @SuppressWarnings("unchecked")
    private void sauvegarderFichesAnalysees(
            Long verdictId,
            List<EvaluateMultipleFichesRequest.FichePayeDTO> fichesRequest,
            Map<String, Object> resultat) {

        Object fichesObj = resultat.get("fiches");
        if (!(fichesObj instanceof List)) {
            throw new RuntimeException("Réponse LLM invalide: champ 'fiches' manquant ou incorrect");
        }

        List<Map<String, Object>> fichesAnalysees = (List<Map<String, Object>>) fichesObj;
        if (fichesAnalysees.size() != fichesRequest.size()) {
            throw new RuntimeException(String.format(
                    "Réponse LLM incohérente: %d fiches analysées attendues, %d reçues",
                    fichesRequest.size(), fichesAnalysees.size()));
        }

        for (int i = 0; i < fichesRequest.size(); i++) {
            Map<String, Object> analyse = fichesAnalysees.get(i);

            FicheAnalysee fiche = new FicheAnalysee();
            fiche.setVerdictId(verdictId);
            fiche.setMois(fichesRequest.get(i).getMois());
            fiche.setSalaireBrut(toBigDecimal(analyse.get("salaireBrut"), null));
            fiche.setSalaireNet(toBigDecimal(analyse.get("salaireNet"), null));
            fiche.setCharges(toBigDecimal(analyse.get("charges"), BigDecimal.ZERO));
            fiche.setImpots(toBigDecimal(analyse.get("impots"), BigDecimal.ZERO));
            fiche.setBonus(toBigDecimal(analyse.get("bonus"), BigDecimal.ZERO));
            fiche.setPrimes(toBigDecimal(analyse.get("primes"), BigDecimal.ZERO));
            fiche.setRevenus_fiables(toBigDecimal(analyse.get("revenusFiables"), BigDecimal.ZERO));
            fiche.setRevenusComplementaires(toBigDecimal(analyse.get("revenusComplementaires"), BigDecimal.ZERO));
            fiche.setStabiliteSalaire((String) analyse.get("stabiliteSalaire"));
            fiche.setDateAnalyse(LocalDateTime.now());

            ficheAnalyseeRepository.save(fiche);
        }
    }

    /**
     * Score moyen de risque (dashboard), calculé sur le dernier verdict de chaque dossier
     * (pour ne pas fausser la moyenne avec les ré-analyses répétées d'un même dossier).
     */
    public Map<String, Object> calculerStatistiquesRisque() {
        List<VerdictCredit> tousLesVerdicts = verdictRepository.findAll();

        Map<Long, VerdictCredit> dernierVerdictParDossier = new HashMap<>();
        for (VerdictCredit v : tousLesVerdicts) {
            VerdictCredit existant = dernierVerdictParDossier.get(v.getDossierId());
            if (existant == null || v.getTimestamp().isAfter(existant.getTimestamp())) {
                dernierVerdictParDossier.put(v.getDossierId(), v);
            }
        }

        if (dernierVerdictParDossier.isEmpty()) {
            Map<String, Object> vide = new HashMap<>();
            vide.put("scoreMoyenRisque", null);
            vide.put("nombreDossiersAnalyses", 0);
            return vide;
        }

        double moyenne = dernierVerdictParDossier.values().stream()
                .mapToInt(VerdictCredit::getScoreRisque)
                .average()
                .orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("scoreMoyenRisque", Math.round(moyenne));
        stats.put("nombreDossiersAnalyses", dernierVerdictParDossier.size());
        return stats;
    }

    private static final BigDecimal TAUX_ENDETTEMENT_MAX = new BigDecimal("0.33");

    /**
     * MontantCreditMax = [(SalaireNet x TauxEndettementMax - ChargesExistantes) x (1 - RisqueImpayement)] x DureeEnMois
     *
     * Calculé en Java (pas par le LLM) pour garantir une arithmétique exacte et reproductible.
     */
    @SuppressWarnings("unchecked")
    private BigDecimal calculerMontantMaxRecommande(Map<String, Object> resultat, int scoreRisque, String dureeMaxRecommandee) {
        Object fichesObj = resultat.get("fiches");
        if (!(fichesObj instanceof List)) {
            return BigDecimal.ZERO;
        }
        List<Map<String, Object>> fichesAnalysees = (List<Map<String, Object>>) fichesObj;
        if (fichesAnalysees.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal salaireNetMoyen = moyenneChamp(fichesAnalysees, "salaireNet");
        BigDecimal chargesMoyennes = moyenneChamp(fichesAnalysees, "charges");
        BigDecimal risqueImpayement = BigDecimal.valueOf(scoreRisque).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        int dureeEnMois = extraireNombreMois(dureeMaxRecommandee);

        BigDecimal capaciteMensuelle = salaireNetMoyen.multiply(TAUX_ENDETTEMENT_MAX).subtract(chargesMoyennes);
        if (capaciteMensuelle.compareTo(BigDecimal.ZERO) < 0) {
            capaciteMensuelle = BigDecimal.ZERO;
        }

        BigDecimal facteurRisque = BigDecimal.ONE.subtract(risqueImpayement);

        return capaciteMensuelle
                .multiply(facteurRisque)
                .multiply(BigDecimal.valueOf(dureeEnMois))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moyenneChamp(List<Map<String, Object>> fiches, String champ) {
        BigDecimal somme = BigDecimal.ZERO;
        int compte = 0;
        for (Map<String, Object> fiche : fiches) {
            Object valeur = fiche.get(champ);
            if (valeur != null) {
                somme = somme.add(new BigDecimal(valeur.toString()));
                compte++;
            }
        }
        return compte > 0 ? somme.divide(BigDecimal.valueOf(compte), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private static final Pattern NOMBRE_MOIS = Pattern.compile("\\d+");

    private int extraireNombreMois(String dureeMaxRecommandee) {
        if (dureeMaxRecommandee == null) return 12;
        Matcher m = NOMBRE_MOIS.matcher(dureeMaxRecommandee);
        return m.find() ? Integer.parseInt(m.group()) : 12;
    }

    private AnalyseCompleteFichesResponse construireReponse(VerdictCredit v) {
        AnalyseCompleteFichesResponse response = new AnalyseCompleteFichesResponse();
        response.setVerdictId(v.getId());
        response.setDossierId(v.getDossierId());
        response.setNombreFichesAnalysees(v.getNombreFichesAnalysees());
        response.setPeriode(v.getPeriode());
        response.setTimestamp(v.getTimestamp());

        response.setScoreRisque(v.getScoreRisque());
        response.setVerdict(v.getVerdict());
        response.setConfiance(v.getConfiance());

        response.setPointsForts(parseJsonList(v.getPointsForts()));
        response.setRisquesMajeurs(parseJsonList(v.getRisquesMajeurs()));
        response.setTendancesObservees(parseJsonList(v.getTendancesObservees()));

        response.setMontantMaxRecommande(v.getMontantMaxRecommande());
        response.setDureeMaxRecommandee(v.getDureeMaxRecommandee());
        response.setConditionsSpeciales(parseJsonList(v.getConditionsSpeciales()));
        response.setTauxInteretRecommande(v.getTauxInteretRecommande());

        response.setJustification(v.getJustification());
        response.setResumeCourt(v.getResumeCourt());

        return response;
    }

    /**
     * Appeler Ollama et extraire le JSON généré.
     *
     * L'API Ollama (/api/generate) renvoie une enveloppe
     * {"model":..., "response": "<texte généré>", "done": true, ...} -
     * le texte généré (dans "response") est ce qui nous intéresse, pas
     * l'enveloppe elle-même. Le LLM peut aussi entourer son JSON de texte
     * ou de balises markdown, d'où l'extraction entre '{' et '}'.
     *
     * Propage l'erreur (pas de fallback silencieux) pour que les échecs LLM
     * se traduisent en erreur claire plutôt qu'en verdict fabriqué.
     */
    @SuppressWarnings("unchecked")
    private String callOllama(String prompt) {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", false,
                "temperature", 0.3
        );

        String enveloppeBrute;
        try {
            enveloppeBrute = restClient.post()
                    .uri(OLLAMA_GENERATE_URL)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur appel Ollama", e);
            throw new RuntimeException("Erreur communication avec Ollama: " + e.getMessage(), e);
        }

        Map<String, Object> enveloppe;
        try {
            enveloppe = objectMapper.readValue(enveloppeBrute, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Réponse Ollama illisible: " + e.getMessage(), e);
        }

        Object texteGenere = enveloppe.get("response");
        if (!(texteGenere instanceof String texte)) {
            throw new RuntimeException("Réponse Ollama sans champ 'response' exploitable");
        }

        return extraireJson(texte);
    }

    private String extraireJson(String texte) {
        int debut = texte.indexOf('{');
        int fin = texte.lastIndexOf('}');
        if (debut == -1 || fin == -1 || fin < debut) {
            throw new RuntimeException("Aucun JSON trouvé dans la réponse du LLM: " + texte);
        }
        return texte.substring(debut, fin + 1);
    }

    private BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            if (defaultValue == null) {
                throw new RuntimeException("Valeur numérique attendue mais absente dans la réponse LLM");
            }
            return defaultValue;
        }
        return new BigDecimal(value.toString());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : List.of());
        } catch (Exception e) {
            return "[]";
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

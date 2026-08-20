"""
Génération des prompts pour Ollama
SPRINT 7: 3 prompts spécialisés pour analyse multi-fiches

Architecture:
- Prompt 1: Analyser UNE fiche individuellement
- Prompt 2: Comparer 3 fiches et extraire tendances
- Prompt 3: Générer verdict final
"""

import logging
import json

logger = logging.getLogger(__name__)


class PromptService:
    """Construit les 3 prompts pour l'analyse multi-fiches"""
    
    # ============ ÉTAPE 1: Analyser UNE fiche ============
    
    @staticmethod
    def prompt_analyser_fiche_individuelle(mois: str, fiche_texte: str) -> str:
        """
        ÉTAPE 1: Analyser UNE seule fiche de paie
        
        Args:
            mois: "Janvier 2024"
            fiche_texte: Texte extrait par OCR (Sprint 6)
        
        Returns:
            Prompt structuré pour Ollama/Llama3
        """
        
        prompt = f"""Vous êtes un expert senior en analyse de fiches de paie pour évaluation crédit bancaire.

FICHE DE PAIE - {mois}
{'='*60}
{fiche_texte}
{'='*60}

TÂCHE: Analyser CETTE FICHE UNIQUEMENT

Instructions:
1. Extraire les montants: Brut, Net, Charges, Impôts, Bonus, Primes
2. Évaluer la stabilité du salaire (stable, variable, bonus-dépendant)
3. Identifier revenus fiables (salaire de base) vs complémentaires
4. Ajouter observations brèves

RÉPONDRE EN JSON STRICTEMENT VALIDE - PAS DE TEXTE AVANT/APRÈS:

{{
    "mois": "{mois}",
    "salaire_brut": <nombre exact ou 0>,
    "salaire_net": <nombre exact ou 0>,
    "charges_sociales": <nombre exact ou 0>,
    "impots": <nombre exact ou 0>,
    "retenues": <nombre exact ou 0>,
    "bonus": <nombre exact ou 0>,
    "primes": <nombre exact ou 0>,
    "stabilite_salaire": "<stable|variable|bonus_dependent>",
    "revenus_fiables": <nombre = salaire_brut minimum>,
    "revenus_complementaires": <nombre = bonus + primes>,
    "notes": "<observations brèves 50 caractères max>"
}}

Analyse maintenant:"""
        
        return prompt
    
    
    # ============ ÉTAPE 2: Comparer 3 fiches ============
    
    @staticmethod
    def prompt_comparer_fiches(analyses_json: str) -> str:
        """
        ÉTAPE 2: Comparer les 3 fiches analysées et extraire tendances
        
        Args:
            analyses_json: JSON string des 3 fiches déjà analysées
        
        Returns:
            Prompt structuré pour Ollama/Llama3
        """
        
        prompt = f"""Vous êtes expert en analyse de tendances salariales pour crédit bancaire.

TROIS FICHES DÉJÀ ANALYSÉES:
{'='*60}
{analyses_json}
{'='*60}

TÂCHE: Comparer les 3 mois et extraire TENDANCES

Instructions:
1. Calculer salaire moyen, min, max, écart-type
2. Identifier tendance: Hausse / Baisse / Stable
3. Évaluer volatilité (0-100): Stable=faible, Variable=haut
4. Calculer ratio revenus variables vs total
5. Évaluer capacité de remboursement
6. Ajouter 3 observations clés

FORMULES:
- salaire_moyen = (sum of salaire_net) / 3
- volatilite = écart-type / moyenne * 100
- ratio_variables = revenus_variables_moyen / (revenus_garantis_moyen + revenus_variables_moyen)
- tendance = croissant si last > first, décroissant si last < first, sinon stable

RÉPONDRE EN JSON STRICTEMENT VALIDE - PAS DE TEXTE AVANT/APRÈS:

{{
    "periode": "<Mois1-Mois3 2024>",
    "nombre_fiches": 3,
    "salaire_moyen": <nombre arrondi à 2 décimales>,
    "salaire_min": <nombre>,
    "salaire_max": <nombre>,
    "salaire_std_dev": <nombre>,
    "tendance": "<hausse|baisse|stable>",
    "volatilite": <nombre 0-100>,
    "revenus_garantis_moyen": <nombre>,
    "revenus_variables_moyen": <nombre>,
    "ratio_variables_sur_total": <nombre 0-1>,
    "capacite_remboursement": "<Excellente|Bonne|Moyenne|Faible>",
    "observations": [
        "<observation 1 : 50 caractères>",
        "<observation 2 : 50 caractères>",
        "<observation 3 : 50 caractères>"
    ]
}}

Analyse maintenant:"""
        
        return prompt
    
    
    # ============ ÉTAPE 3: Générer verdict final ============
    
    @staticmethod
    def prompt_generer_verdict_final(
        analyses_json: str,
        comparaison_json: str,
        dossier_info: dict = None
    ) -> str:
        """
        ÉTAPE 3: Générer le VERDICT FINAL de crédit
        
        Args:
            analyses_json: JSON des 3 fiches individuelles
            comparaison_json: JSON de la comparaison
            dossier_info: Info additionnelle du dossier
        
        Returns:
            Prompt structuré pour Ollama/Llama3
        """
        
        dossier_info = dossier_info or {}
        dossier_info_str = json.dumps(dossier_info, ensure_ascii=False, indent=2)
        
        prompt = f"""Vous êtes expert senior en évaluation de crédit auprès d'une banque (20+ ans expérience).

DONNÉES COMPLÈTES D'ANALYSE:

FICHES INDIVIDUELLES:
{'='*60}
{analyses_json}
{'='*60}

ANALYSE COMPARATIVE:
{'='*60}
{comparaison_json}
{'='*60}

INFOS DOSSIER:
{'='*60}
{dossier_info_str}
{'='*60}

TÂCHE: Générer le VERDICT FINAL (VALIDE / RISQUE / REJETE)

CRITÈRES D'ÉVALUATION:
1. Revenus: Suffisants? Stables? Capacité remboursement?
2. Tendances: Amélioration ou détérioration?
3. Volatilité: Constance ou variations majeures?
4. Bonus/Primes: Garantis ou "best case"?
5. Endettement: Ratio acceptable?

SCORING:
- 0-30:   Faible risque → VALIDE
- 31-70:  Risque modéré → RISQUE
- 71-100: Risque élevé → REJETE

RECOMMANDATIONS - Doivent être SPÉCIFIQUES:
- Montants: "30000€ max", pas "montant convenable"
- Durées: "24 mois", pas "durée standard"
- Conditions: "Taux normal", "Assurance chômage", pas vagues

RÉPONDRE EN JSON STRICTEMENT VALIDE - PAS DE TEXTE AVANT/APRÈS:

{{
    "score_risque": <entier 0-100>,
    "verdict": "<VALIDE|RISQUE|REJETE>",
    "confiance": <décimal 0.0-1.0>,
    "points_forts": [
        "<point fort 1>",
        "<point fort 2>",
        "<point fort 3>"
    ],
    "risques_majeurs": [
        "<risque 1>",
        "<risque 2>"
    ],
    "tendances_observees": [
        "<tendance 1>",
        "<tendance 2>"
    ],
    "montant_max_recommande": <nombre en euros ou null>,
    "duree_max_recommandee": "<12 mois|24 mois|36 mois|48 mois>",
    "conditions_speciales": [
        "<condition 1>",
        "<condition 2>"
    ],
    "taux_interet_recommande": "<Normal|Majoré 0.25%|Majoré 0.5%>",
    "justification": "<Résumé 300-500 caractères expliquant pourquoi ce verdict>",
    "resume_court": "<1-2 lignes court résumé>"
}}

Analyse maintenant - RÉPONDRE UNIQUEMENT EN JSON VALIDE:"""
        
        return prompt
    
    
    # ============ ANCIEN PROMPT (DÉPRÉCIÉ - Compatibilité Sprint 5) ============
    
    @staticmethod
    def construire_prompt_analyse_legacy(
        dossierId: int, 
        clientNom: str, 
        clientPrenom: str,
        documents: list, 
        contexte: str = None
    ) -> str:
        """
        DEPRECATED: Ancien prompt générique (Sprint 5/6)
        Gardé pour compatibilité avec anciens endpoints
        
        ⚠️ NE PAS UTILISER POUR SPRINT 7
        """
        
        logger.warning("DEPRECATED: prompt_analyse_legacy est déprécié. Utiliser les 3 nouveaux prompts.")
        
        docs_text = "\n".join([
            f"- {doc.get('typeDocument', 'Unknown')}: {doc.get('contenu', '')[:500]}"
            for doc in documents
        ])
        
        prompt = f"""Tu es un expert senior en analyse de crédit bancaire avec 20 ans d'expérience.
Analyse le dossier suivant de manière rigoureuse et fournis une évaluation structurée en JSON.

=== DOSSIER ===
ID: {dossierId}
Client: {clientPrenom} {clientNom}

=== DOCUMENTS FOURNIS ===
{docs_text}

=== CONTEXTE SUPPLÉMENTAIRE ===
{contexte or 'Aucun contexte supplémentaire'}

=== INSTRUCTIONS ===
Réponds STRICTEMENT au format JSON suivant, sans aucun texte supplémentaire avant ou après:

{{
  "score_risque": <nombre entre 0 et 100>,
  "verdict": "<VALIDE|RISQUE|REJETE>",
  "justification": "<raison principale en 1-2 phrases>",
  "recommandations": ["<recommandation 1>", "<recommandation 2>"]
}}

=== CRITÈRES D'ÉVALUATION ===
- Score 0-30: Crédit sans risque majeur (VALIDE)
- Score 31-70: Requiert analyse supplémentaire (RISQUE)
- Score 71-100: Risque majeur, rejet probable (REJETE)

Analyse complète maintenant:"""
        
        return prompt


# ============ SINGLETON ============

_prompt_service = None

def get_prompt_service() -> PromptService:
    """Retourne l'instance singleton du service Prompt"""
    global _prompt_service
    if _prompt_service is None:
        _prompt_service = PromptService()
    return _prompt_service
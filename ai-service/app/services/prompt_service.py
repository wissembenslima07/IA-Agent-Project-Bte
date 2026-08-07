"""Génération des prompts pour Ollama"""
import logging

logger = logging.getLogger(__name__)

class PromptService:
    """Construit les prompts pour l'analyse"""
    
    @staticmethod
    def construire_prompt_analyse(dossierId: int, clientNom: str, clientPrenom: str, 
                                  documents: list, contexte: str = None) -> str:
        """Construit le prompt pour l'analyse du dossier"""
        
        # Formater les documents
        docs_text = "\n".join([
            f"- {doc['typeDocument']}: {doc['contenu'][:500]}"
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
  "recommandations": [<liste de 2-3 recommandations spécifiques>]
}}

=== CRITÈRES D'ÉVALUATION ===
- Score 0-30: Crédit sans risque majeur (VALIDE)
- Score 31-70: Requiert analyse supplémentaire (RISQUE)
- Score 71-100: Risque majeur, rejet probable (REJETE)

=== RECOMMANDATIONS DOIVENT ÊTRE SPÉCIFIQUES ET ACTIONNABLES ===
Exemples: "Demander 3 ans de relevés bancaires", "Vérifier CDI signé", "Contre-visite immobilière"

Analyse complète maintenant:"""
        
        logger.info(f"Prompt construit pour dossier {dossierId}")
        return prompt

# Instance singleton
_prompt_service = None

def get_prompt_service() -> PromptService:
    """Retourne l'instance singleton du service Prompt"""
    global _prompt_service
    if _prompt_service is None:
        _prompt_service = PromptService()
    return _prompt_service
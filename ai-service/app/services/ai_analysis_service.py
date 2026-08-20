"""Service d'analyse IA"""
import logging
from app.services.ollama_service import get_ollama_service
from app.services.prompt_service import get_prompt_service
from app.utils.json_parser import extraire_json, valider_analyse_response
from app.utils.exceptions import AnalysisException
from app.schemas.responses import AnalyseResponse

logger = logging.getLogger(__name__)

class AiAnalysisService:
    """Orchestre l'analyse IA"""
    
    def __init__(self):
        self.ollama = get_ollama_service()
        self.prompt = get_prompt_service()
    
    def analyser_dossier(self, dossierId: int, clientNom: str, clientPrenom: str,
                         documents: list, contexte: str = None) -> AnalyseResponse:
        """Lance l'analyse complète d'un dossier"""
        
        try:
            logger.info(f"Analyse lancée pour dossier {dossierId}")
            
            # 1. Construire le prompt
            prompt = self.prompt.construire_prompt_analyse_legacy(
             dossierId, clientNom, clientPrenom, documents, contexte)
            
            # 2. Appeler Ollama
            response_text = self.ollama.generer(prompt)
            
            # 3. Parser le JSON
            parsed = extraire_json(response_text)
            
            # 4. Valider
            validated = valider_analyse_response(parsed)
            
            # 5. Construire la réponse structurée
            analyse = AnalyseResponse(
                dossierId=dossierId,
                score_risque=validated['score_risque'],
                verdict=validated['verdict'],
                justification=validated['justification'],
                recommandations=validated['recommandations']
            )
            
            logger.info(f"Analyse complétée: dossierId={dossierId}, "
                       f"verdict={analyse.verdict}, score={analyse.score_risque}")
            
            return analyse
            
        except Exception as e:
            logger.error(f"Erreur analyse dossier {dossierId}: {e}")
            raise AnalysisException(f"Erreur analyse IA: {str(e)}")

# Instance singleton
_analysis_service = None

def get_analysis_service() -> AiAnalysisService:
    """Retourne l'instance singleton du service d'analyse"""
    global _analysis_service
    if _analysis_service is None:
        _analysis_service = AiAnalysisService()
    return _analysis_service
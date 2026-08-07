"""Endpoints d'analyse"""
import logging
from fastapi import APIRouter, HTTPException
from app.schemas.analyse import AnalyseRequest
from app.schemas.responses import AnalyseResponse
from app.services.ai_analysis_service import get_analysis_service
from app.utils.validation import valider_analyse_request, ValidationError
from app.utils.exceptions import AnalysisException, OllamaException

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/analyse", tags=["Analysis"])

@router.post("", response_model=AnalyseResponse, status_code=200)
async def analyser_dossier(request: AnalyseRequest):
    """
    Lance une analyse IA d'un dossier de crédit.
    
    - **dossierId**: ID unique du dossier
    - **clientNom**: Nom du client
    - **clientPrenom**: Prénom du client
    - **documents**: Liste des documents à analyser
    - **contexteSupplementaire**: Contexte optionnel (emploi, secteur, etc.)
    
    Retourne un score de risque (0-100), un verdict et des recommandations.
    """
    try:
        # Valider les données
        docs_list = [doc.dict() for doc in request.documents]
        valider_analyse_request(request.dossierId, docs_list)
        
        # Lancer l'analyse
        service = get_analysis_service()
        analyse = service.analyser_dossier(
            dossierId=request.dossierId,
            clientNom=request.clientNom,
            clientPrenom=request.clientPrenom,
            documents=docs_list,
            contexte=request.contexteSupplementaire
        )
        
        return analyse
        
    except ValidationError as e:
        logger.warning(f"Validation error: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    except OllamaException as e:
        logger.error(f"Ollama error: {e}")
        raise HTTPException(status_code=503, detail="Service IA indisponible")
    except AnalysisException as e:
        logger.error(f"Analysis error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur")
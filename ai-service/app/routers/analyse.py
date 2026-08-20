"""
Endpoints d'analyse
SPRINT 7: Support analyse multi-fiches + ancien endpoint compatibilité
"""

import logging
import re
from typing import List, Optional
from fastapi import APIRouter, HTTPException, UploadFile, File, Form

from app.schemas.analyse import AnalyseRequest
from app.schemas.responses import AnalyseResponse
from app.schemas.credit import AnalyseCompleteFiches
from app.services.ai_analysis_service import get_analysis_service
from app.services.credit_analysis_service import get_credit_service
from app.services.ocr_service import get_ocr_service
from app.utils.validation import valider_analyse_request, ValidationError
from app.utils.exceptions import AnalysisException, OllamaException

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/analyse", tags=["Analysis"])


# ============ ANCIEN ENDPOINT (Sprint 5/6) - Compatibilité ============

@router.post("", response_model=AnalyseResponse, status_code=200)
async def analyser_dossier(request: AnalyseRequest):
    """
    Lance une analyse IA d'un dossier de crédit (ANCIEN - Sprint 5/6).

    ⚠️ Pour analyses multi-fiches, utiliser: POST /api/analyse/evaluate-multiple-fiches

    - **dossierId**: ID unique du dossier
    - **clientNom**: Nom du client
    - **clientPrenom**: Prénom du client
    - **documents**: Liste des documents à analyser
    - **contexteSupplementaire**: Contexte optionnel (emploi, secteur, etc.)

    Retourne:
    - score_risque (0-100)
    - verdict (VALIDE|RISQUE|REJETE)
    - recommandations
    """
    try:
        logger.info(f"📋 POST /analyse - Dossier {request.dossierId}")

        docs_list = [doc.dict() for doc in request.documents]
        valider_analyse_request(request.dossierId, docs_list)

        service = get_analysis_service()
        analyse = service.analyser_dossier(
            dossierId=request.dossierId,
            clientNom=request.clientNom,
            clientPrenom=request.clientPrenom,
            documents=docs_list,
            contexte=request.contexteSupplementaire
        )

        logger.info(f"✅ Analyse complète: {analyse.verdict}")
        return analyse

    except ValidationError as e:
        logger.warning(f"❌ Validation error: {e}")
        raise HTTPException(status_code=400, detail=str(e))

    except OllamaException as e:
        logger.error(f"❌ Ollama error: {e}")
        raise HTTPException(status_code=503, detail="Service IA indisponible")

    except AnalysisException as e:
        logger.error(f"❌ Analysis error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

    except Exception as e:
        logger.error(f"❌ Unexpected error: {type(e).__name__}: {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur")


# ============ NOUVEL ENDPOINT (Sprint 7) - Analyse multi-fiches ============

@router.post(
    "/evaluate-multiple-fiches",
    response_model=AnalyseCompleteFiches,
    status_code=200,
    summary="Analyser 3 fiches de paie et générer verdict crédit"
)
async def evaluate_multiple_fiches(
    files: List[UploadFile] = File(..., description="2-12 fiches de paie (PDF ou images)"),
    dossierId: int = Form(..., description="ID du dossier de crédit"),
    clientName: Optional[str] = Form(None, description="Nom du client"),
    clientEmail: Optional[str] = Form(None, description="Email du client")
):
    """
    🆕 SPRINT 7: Analyser PLUSIEURS fiches de paie et générer verdict de crédit

    **Pipeline complet (4 étapes):**

    1. **Extraction OCR** (Sprint 6)
       - pdfplumber pour PDF texte
       - PaddleOCR pour images/scans

    2. **Analyse individuelles** (ÉTAPE 1)
       - LLM analyse chaque fiche
       - Résultat: Salaires, stabilité, revenus fiables

    3. **Comparaison & Tendances** (ÉTAPE 2)
       - LLM compare les fiches
       - Résultat: Moyennes, tendances, volatilité

    4. **Verdict Final** (ÉTAPE 3)
       - LLM génère verdict: VALIDE/RISQUE/REJETE
       - Résultat: Score, recommandations, justification

    **Temps réponse:** 10-15 secondes

    ⚠️ STUB TEMPORAIRE: implémentation réelle du pipeline à venir
    (en attente de credit_analysis_service.py et schemas/credit.py).
    """
    logger.warning(
        f"⚠️ /evaluate-multiple-fiches appelé (dossier {dossierId}) "
        f"mais pas encore implémenté — {len(files)} fichier(s) reçu(s), ignorés."
    )
    raise HTTPException(
        status_code=501,
        detail="Endpoint /evaluate-multiple-fiches en cours d'implémentation (Sprint 7)"
    )
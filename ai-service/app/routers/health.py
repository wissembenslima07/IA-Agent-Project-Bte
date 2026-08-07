"""Endpoints de santé"""
import logging
from fastapi import APIRouter, HTTPException
from app.config import get_settings
from app.services.ollama_service import get_ollama_service
from app.schemas.responses import HealthResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/health", tags=["Health"])
settings = get_settings()

@router.get("", response_model=HealthResponse)
async def health_check():
    """Vérifie l'état de santé du service"""
    try:
        ollama = get_ollama_service()
        ollama_available = ollama.verifier_disponibilite()
    except Exception as e:
        logger.warning(f"Ollama indisponible: {e}")
        ollama_available = False
    
    return HealthResponse(
        status="healthy" if ollama_available else "degraded",
        ollama_available=ollama_available,
        model=settings.OLLAMA_MODEL,
        version=settings.APP_VERSION
    )

@router.get("/ready")
async def readiness_check():
    """Readiness probe pour Kubernetes"""
    try:
        ollama = get_ollama_service()
        ollama.verifier_disponibilite()
        return {"status": "ready"}
    except Exception as e:
        raise HTTPException(status_code=503, detail="Service not ready")
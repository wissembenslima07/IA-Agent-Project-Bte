# ============ BTE CREDIT ANALYSIS SERVICE ============
# Intelligent Credit Dossier Analysis System
# FastAPI Microservice + Ollama + LangGraph + Sprint 7
# ============================================================

from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from contextlib import asynccontextmanager
import logging
from typing import Optional
import json

from app.routers import health, analyse, ocr
from app.services.ocr_service import get_ocr_service
from app.services.llm_service import get_llm_service
from app.services.credit_analysis_service import get_credit_service

# ============ LOGGING ============
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ============ LIFESPAN ============
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Gestion du cycle de vie de l'application FastAPI

    Startup:
      - Initialiser OCR Service (Sprint 6)
      - Initialiser LLM Service (Sprint 7)
      - Initialiser Credit Analysis Service (Sprint 7)

    Shutdown:
      - Cleanup
    """

    # ============ STARTUP ============
    logger.info("🚀 BTE Credit Analysis Service démarrage...")

    # Initialize OCR Service (Sprint 6)
    try:
        ocr_service = get_ocr_service()
        logger.info("✅ OCR Service initialisé (pdfplumber + PaddleOCR)")
    except Exception as e:
        logger.error(f"❌ Erreur initialisation OCR Service: {e}")

    # ============ NEW Sprint 7: Initialize LLM Service ============
    try:
        logger.info("🔌 Initialisation LLM Service (Ollama/Llama3)...")
        llm_service = get_llm_service()

        # Test connection à Ollama
        is_connected = await llm_service.test_connection()

        if is_connected:
            logger.info("✅ LLM Service initialisé - Ollama accessible")
        else:
            logger.warning("⚠️  LLM Service initialisé MAIS Ollama inaccessible")
            logger.warning("   Endpoints d'analyse multi-fiches échoueront")
            logger.warning("   Vérifier: Ollama running? http://localhost:11434 accessible?")

    except Exception as e:
        logger.error(f"❌ Erreur initialisation LLM Service: {type(e).__name__}: {e}")
        logger.warning("   Endpoints d'analyse multi-fiches ne fonctionneront pas")

    # ============ NEW Sprint 7: Initialize Credit Analysis Service ============
    try:
        logger.info("📊 Initialisation Credit Analysis Service...")
        credit_service = get_credit_service()
        logger.info("✅ Credit Analysis Service initialisé (Pipeline 3-étapes)")

    except Exception as e:
        logger.error(f"❌ Erreur initialisation Credit Service: {type(e).__name__}: {e}")
        logger.warning("   Endpoint /evaluate-multiple-fiches ne fonctionnera pas")

    logger.info("=" * 60)
    logger.info("✅ BTE Credit Analysis Service PRÊT")
    logger.info("=" * 60)

    yield

    logger.info("🛑 BTE Credit Analysis Service arrêt...")
    logger.info("   - OCR Service: cleanup")
    logger.info("   - LLM Service: fermeture")
    logger.info("   - Credit Analysis Service: cleanup")
    logger.info("✅ Arrêt complet")


# ============ FASTAPI APP ============
app = FastAPI(
    title="BTE Credit Analysis Service",
    description="Microservice FastAPI pour analyse de dossiers de crédit avec IA",
    version="1.0.0 (Sprint 7)",
    lifespan=lifespan
)

# ============ CORS ============
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",      # Angular dev
        "http://localhost:8080",      # Alternative
        "http://localhost:3000",      # Node
        "http://127.0.0.1:4200",      # Local
        "http://127.0.0.1:8080",      # Local
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============ ROUTERS ============
app.include_router(health.router)
app.include_router(analyse.router, prefix="/api")
app.include_router(ocr.router, prefix="/api")

# ============ ROOT ENDPOINT ============
@app.get("/")
async def root():
    """Root endpoint - Information du service"""
    return {
        "service": "BTE Credit Analysis Service",
        "version": "1.0.0 (Sprint 7)",
        "status": "🚀 Running",
        "docs": "/docs",
        "health": "/health",
        "api": {
            "analyse_ancien": "POST /api/analyse (Sprint 5/6 - Compatibilité)",
            "analyse_multi_fiches": "POST /api/analyse/evaluate-multiple-fiches (NEW Sprint 7)",
            "ocr": "POST /api/ocr/extract (Sprint 6)",
            "health": "GET /health"
        },
        "features": {
            "Sprint 6": "OCR extraction (pdfplumber + PaddleOCR)",
            "Sprint 7": "Multi-fiche analysis (3-step pipeline with Ollama/Llama3)",
            "Features": [
                "Analyse 2-12 fiches de paie",
                "Extraction OCR automatique",
                "Analyse individuelle par LLM",
                "Comparaison et tendances",
                "Verdict final avec score de risque",
                "Recommandations spécifiques"
            ]
        }
    }


# ============ TAGS OPENAPI ============
tags_metadata = [
    {
        "name": "health",
        "description": "Vérification de l'état du service et des dépendances",
    },
    {
        "name": "Analysis",
        "description": "Analyse de dossiers de crédit avec IA (Ollama/Llama3)",
        "externalDocs": {
            "description": "Pipeline d'analyse: 3 étapes de traitement IA",
            "url": "https://github.com/bte-credit",
        },
    },
    {
        "name": "OCR",
        "description": "Extraction de texte depuis PDF/images (Sprint 6)",
        "externalDocs": {
            "description": "Traitement du texte via pdfplumber (PDF texte) et PaddleOCR (images/scans)",
            "url": "https://github.com/bte-credit",
        },
    },
]

app.openapi_tags = tags_metadata


# ============ EXCEPTION HANDLERS ============
# IMPORTANT: un exception handler FastAPI/Starlette DOIT retourner un objet
# Response (ex: JSONResponse) et non un dict brut. Retourner un dict casse
# l'envoi de la réponse (TypeError: 'dict' object is not callable) et masque
# la vraie erreur métier derrière un message générique "Internal Server Error".

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Gestion des exceptions HTTP"""
    logger.error(f"HTTP {exc.status_code}: {exc.detail}")
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": True,
            "status_code": exc.status_code,
            "detail": exc.detail
        }
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """Gestion des exceptions générales"""
    logger.error(f"Unexpected error: {type(exc).__name__}: {exc}")
    return JSONResponse(
        status_code=500,
        content={
            "error": True,
            "status_code": 500,
            "detail": "Erreur interne du serveur"
        }
    )


# ============ MIDDLEWARE LOGGING ============

@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log toutes les requêtes HTTP"""

    # Éviter de logger les health checks trop souvent
    if request.url.path == "/health":
        response = await call_next(request)
        return response

    logger.info(f"📤 {request.method} {request.url.path}")

    response = await call_next(request)

    logger.info(f"📥 {request.method} {request.url.path} → {response.status_code}")

    return response


if __name__ == "__main__":
    import uvicorn

    logger.info("Starting BTE Credit Analysis Service...")
    logger.info("API Documentation: http://localhost:8000/docs")

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=False,
        log_level="info"
    )
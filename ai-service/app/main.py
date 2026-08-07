"""Point d'entrée FastAPI - Analyse IA + OCR/Extraction"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings
from app.routers import health, analyse, ocr  # ← Ajouter OCR
from app.services.ollama_service import get_ollama_service

settings = get_settings()
logger = logging.getLogger(__name__)

# Lifecycle events
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Gère le démarrage et l'arrêt de l'application"""
    # ============ STARTUP ============
    logger.info(f"🚀 Démarrage {settings.APP_NAME} v{settings.APP_VERSION}")
    
    # Vérifier Ollama (pour analyses IA)
    try:
        ollama = get_ollama_service()
        ollama.verifier_disponibilite()
        logger.info("✅ Ollama disponible")
    except Exception as e:
        logger.warning(f"⚠️ Ollama indisponible au démarrage: {e}")
    
    # Initialiser OCR (peut être lent au premier appel)
    try:
        from app.services.ocr_service import get_ocr_service
        ocr_service = get_ocr_service()
        logger.info("✅ Service OCR initialisé")
    except Exception as e:
        logger.warning(f"⚠️ Erreur initialisation OCR: {e}")
    
    logger.info(f"🎯 Services disponibles: Analyse IA, OCR/Extraction")
    
    yield
    
    # ============ SHUTDOWN ============
    logger.info("🛑 Arrêt du service")

# Créer l'app
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Microservice IA: Analyse crédit + OCR/Extraction documentaire",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # À restreindre en production : ["http://localhost:4200", "http://localhost:8080"]
    allow_credentials=True,
    allow_methods=["*"],  # À restreindre: ["GET", "POST", "PUT", "DELETE"]
    allow_headers=["*"],  # À restreindre: ["Content-Type", "Authorization"]
)

# ============ ROUTERS ============
# Health checks
app.include_router(health.router)

# Analyse IA (Sprint 5)
app.include_router(analyse.router, prefix="/api")

# OCR/Extraction (Sprint 6)
app.include_router(ocr.router, prefix="/api")

# ============ ROOT ENDPOINT ============
@app.get("/")
async def root():
    """Endpoint racine - informations du service"""
    return {
        "name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "running",
        "services": {
            "analyse_ia": {
                "endpoint": "/api/analyse",
                "method": "POST",
                "description": "Analyse de dossiers de crédit avec Ollama/Llama3"
            },
            "ocr_extraction": {
                "endpoint": "/api/ocr/extract",
                "method": "POST",
                "description": "Extraction de texte depuis PDF/images (PDF texte + PaddleOCR)"
            }
        },
        "documentation": "/docs",
        "health": "/health"
    }

# ============ ENDPOINTS INFO ============
@app.get("/info")
async def info():
    """Informations détaillées du service"""
    return {
        "app": {
            "name": settings.APP_NAME,
            "version": settings.APP_VERSION,
            "debug": settings.DEBUG
        },
        "ai": {
            "ollama_url": settings.OLLAMA_BASE_URL,
            "model": settings.OLLAMA_MODEL,
            "timeout": settings.OLLAMA_TIMEOUT
        },
        "ocr": {
            "supported_formats": ["application/pdf", "image/png", "image/jpeg", "image/tiff", "image/gif"],
            "max_file_size_mb": 50,
            "methods": ["pdf_text (PyMuPDF)", "ocr_paddle (PaddleOCR)"]
        }
    }

# ============ HEALTH CHECK ENDPOINTS ============
@app.get("/docs", include_in_schema=False)
async def docs():
    """Swagger UI - Accès automatique via /docs"""
    pass

@app.get("/redoc", include_in_schema=False)
async def redoc():
    """ReDoc UI - Accès automatique via /redoc"""
    pass

# ============ ERROR HANDLERS ============
@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Gestionnaire d'erreurs global"""
    logger.error(f"Erreur non gérée: {exc}", exc_info=True)
    return {
        "detail": "Erreur interne du serveur",
        "type": type(exc).__name__
    }

# ============ MAIN ============
if __name__ == "__main__":
    import uvicorn
    logger.info("Démarrage du serveur Uvicorn...")
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.DEBUG,
        log_level="info"
    )
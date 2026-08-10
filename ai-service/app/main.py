# ============ BTE CREDIT ANALYSIS SERVICE ============
# Intelligent Credit Dossier Analysis System
# FastAPI Microservice + Ollama + LangGraph
# ============================================================

from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from contextlib import asynccontextmanager
import logging
from typing import Optional
import json

from app.routers import health, analyse, ocr
from app.services.ocr_service import get_ocr_service

# ============ LOGGING ============
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ============ LIFESPAN ============
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("🚀 BTE Credit Analysis Service démarrage...")
    
    # Initialize OCR Service
    try:
        ocr_service = get_ocr_service()
        logger.info("✅ OCR Service initialisé")
    except Exception as e:
        logger.error(f"❌ Erreur initialisation OCR Service: {e}")
    
    yield
    
    # Shutdown
    logger.info("🛑 BTE Credit Analysis Service arrêt...")

# ============ FASTAPI APP ============
app = FastAPI(
    title="BTE Credit Analysis Service",
    description="Microservice FastAPI pour analyse de dossiers de crédit avec IA",
    version="1.0.0",
    lifespan=lifespan
)

# ============ CORS ============
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============ ROUTERS ============
app.include_router(health.router)
app.include_router(analyse.router, prefix="/api")
app.include_router(ocr.router, prefix="/api")

# ============ OPENAPI DOCUMENTATION ============
@app.get("/")
async def root():
    """Root endpoint - redirection vers docs"""
    return {
        "service": "BTE Credit Analysis Service",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
        "api": {
            "analyse": "POST /api/analyse/evaluate",
            "ocr": "POST /api/ocr/extract"
        }
    }

# ============ TAGS OPENAPI ============
tags_metadata = [
    {
        "name": "health",
        "description": "Vérification de l'état du service",
    },
    {
        "name": "analyse",
        "description": "Analyse de dossiers de crédit avec IA (Ollama/Llama3)",
    },
    {
        "name": "ocr",
        "description": "Extraction de texte depuis PDF/images",
        "externalDocs": {
            "description": "Traitement du texte via pdfplumber (PDF texte) et PaddleOCR (images/scans)",
            "url": "https://github.com/bte-credit",
        },
    },
]

app.openapi_tags = tags_metadata

# ============ DETAILED API ENDPOINTS ============

# Endpoint OCR - Extraction de documents
# Supports:
#   - PDF texte (via pdfplumber)
#   - Images JPG, PNG, TIFF (via PaddleOCR)
# Retourne:
#   - Texte extrait
#   - Méthode utilisée (pdf_text ou ocr_paddle)
#   - Score de confiance
"""Schemas de réponses"""
from pydantic import BaseModel, Field
from typing import List
from enum import Enum

class VerdictEnum(str, Enum):
    VALIDE = "VALIDE"
    RISQUE = "RISQUE"
    REJETE = "REJETE"

class AnalyseResponse(BaseModel):
    """Réponse d'analyse"""
    dossierId: int
    score_risque: int = Field(..., ge=0, le=100)
    verdict: VerdictEnum
    justification: str
    recommandations: List[str]
    
    class Config:
        json_schema_extra = {
            "example": {
                "dossierId": 1,
                "score_risque": 42,
                "verdict": "VALIDE",
                "justification": "Profil stable avec revenus réguliers",
                "recommandations": ["Demander 2 ans de relevés bancaires", "Vérifier CDI"]
            }
        }

class HealthResponse(BaseModel):
    """État de santé du service"""
    status: str
    ollama_available: bool
    model: str
    version: str
    
    class Config:
        json_schema_extra = {
            "example": {
                "status": "healthy",
                "ollama_available": True,
                "model": "llama3:latest",
                "version": "1.0.0"
            }
        }

class ErrorResponse(BaseModel):
    """Erreur"""
    detail: str
    code: str
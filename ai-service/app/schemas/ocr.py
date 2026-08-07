"""Schemas OCR"""
from pydantic import BaseModel, Field
from typing import Optional, List

class DocumentOCRRequest(BaseModel):
    """Requête d'extraction OCR"""
    documentId: int = Field(..., description="ID du document uploadé")
    nomFichier: str = Field(..., description="Nom du fichier original")
    mimeType: str = Field(..., description="Type MIME (application/pdf, image/png, etc)")

class ElementExtraitOCR(BaseModel):
    """Élément extrait (pour images/OCR)"""
    texte: str
    confidence: float = Field(..., ge=0, le=1)

class DonneesExtraites(BaseModel):
    """Données extraites d'un document"""
    documentId: int
    textComplet: str = Field(..., description="Texte complet extrait")
    methode: str = Field(..., description="Méthode utilisée: pdf_text, ocr_paddle")
    confidenceMoyenne: float = Field(..., ge=0, le=1)
    nombrePages: Optional[int] = None
    nombreElements: Optional[int] = None
    elements: Optional[List[ElementExtraitOCR]] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "documentId": 1,
                "textComplet": "Fiche de paie...",
                "methode": "pdf_text",
                "confidenceMoyenne": 1.0,
                "nombrePages": 1
            }
        }

class OCRResponse(BaseModel):
    """Réponse d'extraction"""
    success: bool
    documentId: int
    donnees: Optional[DonneesExtraites] = None
    error: Optional[str] = None
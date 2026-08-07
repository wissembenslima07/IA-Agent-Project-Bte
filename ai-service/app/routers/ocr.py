"""Endpoints OCR/Extraction"""
import logging
from fastapi import APIRouter, HTTPException, UploadFile, File
from app.services.ocr_service import get_ocr_service
from app.schemas.ocr import OCRResponse, DonneesExtraites
from app.utils.exceptions import AnalysisException

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/ocr", tags=["OCR"])

@router.post("/extract", response_model=OCRResponse, status_code=200)
async def extraire_document(
    file: UploadFile = File(...),
    documentId: int = None
):
    """
    Extrait le texte d'un document (PDF ou image).
    
    Supporte:
    - PDF texte (via PyMuPDF)
    - Images JPG, PNG, TIFF (via PaddleOCR)
    
    Returns:
    - Texte complet extrait
    - Confiance d'extraction (0-1)
    - Méthode utilisée (pdf_text, ocr_paddle)
    - Pages/éléments individuels
    """
    
    try:
        if not documentId:
            raise HTTPException(status_code=400, detail="documentId requis")
        
        # Lire le fichier
        content = await file.read()
        
        if not content:
            raise HTTPException(status_code=400, detail="Fichier vide")
        
        # Extraction
        service = get_ocr_service()
        result = service.extraire_document(
            file_bytes=content,
            nom_fichier=file.filename,
            mime_type=file.content_type,
            document_id=documentId
        )
        
        if result["success"]:
            return OCRResponse(
                success=True,
                documentId=documentId,
                donnees=result["donnees"]
            )
        else:
            return OCRResponse(
                success=False,
                documentId=documentId,
                error=result["error"]
            )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Erreur extraction: {e}")
        raise HTTPException(status_code=500, detail=f"Erreur: {str(e)}")

@router.post("/extract/url")
async def extraire_depuis_url(
    dossierId: int,
    documentId: int,
    cheminFichier: str
):
    """
    Extrait le texte d'un document depuis le stockage local.
    
    Args:
        dossierId: ID du dossier
        documentId: ID du document
        cheminFichier: Chemin du fichier dans le stockage
    """
    
    try:
        import os
        from pathlib import Path
        
        # Reconstruire le chemin
        storage_path = os.getenv("STORAGE_PATH", "/uploads")
        file_path = Path(storage_path) / f"dossier_{dossierId}" / cheminFichier
        
        if not file_path.exists():
            raise HTTPException(status_code=404, detail="Fichier non trouvé")
        
        # Lire et extraire
        with open(file_path, "rb") as f:
            content = f.read()
        
        service = get_ocr_service()
        result = service.extraire_document(
            file_bytes=content,
            nom_fichier=file_path.name,
            mime_type=_detect_mime_type(file_path.suffix),
            document_id=documentId
        )
        
        return OCRResponse(
            success=result["success"],
            documentId=documentId,
            donnees=result.get("donnees"),
            error=result.get("error")
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Erreur extraction URL: {e}")
        raise HTTPException(status_code=500, detail=str(e))

def _detect_mime_type(extension: str) -> str:
    """Détecte le type MIME par extension"""
    mapping = {
        ".pdf": "application/pdf",
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".tiff": "image/tiff",
        ".tif": "image/tiff",
        ".gif": "image/gif"
    }
    return mapping.get(extension.lower(), "application/octet-stream")
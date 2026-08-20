"""
Routeur FastAPI pour OCR/Extraction de documents
- POST /api/ocr/extract: Upload et extraction
- POST /api/ocr/extract/url: Extraction via chemin fichier
"""

from fastapi import APIRouter, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
import logging

from app.services.ocr_service import get_ocr_service
from app.schemas.ocr import OCRResponse, DonneesExtraites

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/ocr",
    tags=["ocr"],
    responses={404: {"description": "Not found"}},
)

# ============ DOCUMENTATION ============
"""
🔍 OCR / EXTRACTION DE DOCUMENTS

Endpoints pour extraction de texte depuis:
- PDF texte (via pdfplumber)
- Images JPG, PNG, TIFF (via PaddleOCR / pytesseract)

Méthodes:
- pdf_text: Extraction native depuis PDF (confiance: 1.0)
- ocr_paddle: Reconnaissance optique PaddleOCR (confiance: 0.8-0.95)
- ocr_tesseract: Reconnaissance optique Tesseract (fallback)

Formats supportés:
- application/pdf
- image/png
- image/jpeg
- image/tiff
- image/gif
"""


def _build_ocr_response(result: dict, document_id: int) -> OCRResponse:
    """
    Construit une OCRResponse à partir du résultat renvoyé par ocr_service.

    IMPORTANT: ocr_service.extraire_document() renvoie une structure imbriquée
    {"success": ..., "documentId": ..., "donnees": {...}} — les champs texte/méthode/
    confiance sont DANS "donnees", pas au premier niveau. Centraliser cette lecture
    ici évite de dupliquer (et de re-casser) la logique dans chaque endpoint.
    """
    if not result.get("success"):
        return OCRResponse(
            success=False,
            documentId=document_id,
            error=result.get("error", "Extraction failed")
        )

    donnees = result.get("donnees") or {}

    return OCRResponse(
        success=True,
        documentId=document_id,
        donnees=DonneesExtraites(
            documentId=document_id,
            textComplet=donnees.get("textComplet", ""),
            methode=donnees.get("methode", "unknown"),
            confidenceMoyenne=donnees.get("confidenceMoyenne", 0.0),
            nombrePages=donnees.get("nombrePages"),
            nombreElements=donnees.get("nombreElements")
        )
    )


# ============ ENDPOINTS ============

@router.post(
    "/extract",
    response_model=OCRResponse,
    summary="Extraire du texte d'un fichier uploadé",
    description="Upload un PDF ou une image pour extraction de texte",
    tags=["ocr"]
)
async def extract_file(
    file: UploadFile = File(...),
    documentId: int = Form(...),
):
    """
    Extraction de texte depuis un fichier uploadé

    **Paramètres:**
    - file: Fichier PDF ou image (PNG, JPEG, TIFF, GIF)
    - documentId: ID du document en base de données

    **Réponse:**
    - success: Booléen du succès
    - documentId: ID du document
    - donnees: Données extraites (texte, méthode, confiance)
    - error: Message d'erreur si applicable

    **Méthodes:**
    - pdf_text: PDF texte (confiance: 1.0)
    - ocr_paddle: OCR PaddleOCR (confiance: 0.8-0.95)
    - ocr_tesseract: OCR Tesseract (fallback)
    """

    if not file:
        raise HTTPException(status_code=400, detail="Fichier manquant")

    try:
        file_bytes = await file.read()

        logger.info(f"Extraction: {file.filename} (ID: {documentId})")

        ocr_service = get_ocr_service()
        result = ocr_service.extraire_document(
            file_bytes=file_bytes,
            nom_fichier=file.filename,
            mime_type=file.content_type,
            document_id=documentId
        )

        return _build_ocr_response(result, documentId)

    except Exception as e:
        logger.error(f"❌ Erreur extraction: {e}")
        return OCRResponse(
            success=False,
            documentId=documentId,
            error=str(e)
        )


@router.post(
    "/extract/url",
    response_model=OCRResponse,
    summary="Extraire du texte via chemin fichier",
    description="Extraction depuis un fichier stocké localement",
    tags=["ocr"]
)
async def extract_from_url(
    dossierId: int = Form(...),
    documentId: int = Form(...),
    cheminFichier: str = Form(...)
):
    """
    Extraction de texte depuis un chemin de fichier

    **Paramètres:**
    - dossierId: ID du dossier de crédit
    - documentId: ID du document
    - cheminFichier: Chemin complet du fichier (ex: /uploads/dossier_1/document.pdf)

    **Réponse:**
    - success: Booléen
    - documentId: ID du document
    - donnees: Données extraites
    - error: Message d'erreur si applicable
    """

    try:
        import os
        from pathlib import Path

        if not os.path.exists(cheminFichier):
            raise HTTPException(status_code=404, detail="Fichier non trouvé")

        with open(cheminFichier, 'rb') as f:
            file_bytes = f.read()

        ext = Path(cheminFichier).suffix.lower()
        mime_map = {
            '.pdf': 'application/pdf',
            '.png': 'image/png',
            '.jpg': 'image/jpeg',
            '.jpeg': 'image/jpeg',
            '.tiff': 'image/tiff',
            '.gif': 'image/gif'
        }
        mime_type = mime_map.get(ext, 'application/octet-stream')

        logger.info(f"Extraction URL: {cheminFichier} (ID doc: {documentId})")

        ocr_service = get_ocr_service()
        result = ocr_service.extraire_document(
            file_bytes=file_bytes,
            nom_fichier=os.path.basename(cheminFichier),
            mime_type=mime_type,
            document_id=documentId
        )

        return _build_ocr_response(result, documentId)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Erreur extraction URL: {e}")
        return OCRResponse(
            success=False,
            documentId=documentId,
            error=str(e)
        )
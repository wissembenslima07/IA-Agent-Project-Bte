"""Service d'extraction OCR"""
import logging
from app.utils.ocr_utils import get_extractor
from app.utils.exceptions import AnalysisException

logger = logging.getLogger(__name__)

class OCRExtractionService:
    """Service d'extraction OCR/PDF"""
    
    def __init__(self):
        self.extractor = get_extractor()
    
    def extraire_document(self, file_bytes: bytes, nom_fichier: str, 
                         mime_type: str, document_id: int) -> dict:
        """
        Extrait le texte d'un document
        
        Args:
            file_bytes: Contenu du fichier en bytes
            nom_fichier: Nom du fichier
            mime_type: Type MIME
            document_id: ID du document en base
        
        Returns:
            Dictionnaire avec texte extrait et métadonnées
        """
        try:
            logger.info(f"Extraction du document {document_id} ({nom_fichier})")
            
            # Extraction
            result = self.extractor.extraire(file_bytes, nom_fichier, mime_type)
            
            if not result.get("success"):
                logger.error(f"Extraction échouée: {result.get('error')}")
                return {
                    "success": False,
                    "documentId": document_id,
                    "error": result.get("error")
                }
            
            # Construire la réponse structurée
            donnees = {
                "documentId": document_id,
                "textComplet": result.get("texte_complet", ""),
                "methode": result.get("method"),
                "confidenceMoyenne": result.get("confidence_moyenne", 0),
                "nombrePages": result.get("nombre_pages"),
                "nombreElements": result.get("nombre_elements"),
                "elements": result.get("elements")
            }
            
            return {
                "success": True,
                "documentId": document_id,
                "donnees": donnees
            }
            
        except Exception as e:
            logger.error(f"Erreur extraction document {document_id}: {e}")
            return {
                "success": False,
                "documentId": document_id,
                "error": f"Erreur extraction: {str(e)}"
            }

# Instance singleton
_ocr_service = None

def get_ocr_service() -> OCRExtractionService:
    """Retourne l'instance singleton du service OCR"""
    global _ocr_service
    if _ocr_service is None:
        _ocr_service = OCRExtractionService()
    return _ocr_service
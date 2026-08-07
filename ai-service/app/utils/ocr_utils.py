"""Utilitaires OCR"""
import logging
from io import BytesIO
from pathlib import Path
import pymupdf  # PyMuPDF

logger = logging.getLogger(__name__)

class PDFProcessor:
    """Traitement des fichiers PDF"""
    
    @staticmethod
    def est_pdf_texte(file_bytes: bytes) -> bool:
        """
        Détermine si un PDF contient du texte extractible
        (vs. image/scan)
        """
        try:
            doc = pymupdf.open(stream=file_bytes, filetype="pdf")
            
            # Extraire le texte de la première page
            if len(doc) > 0:
                first_page = doc[0]
                text = first_page.get_text()
                doc.close()
                
                # Si > 50 caractères, c'est un PDF texte
                return len(text.strip()) > 50
            
            doc.close()
            return False
            
        except Exception as e:
            logger.error(f"Erreur détection PDF: {e}")
            return False
    
    @staticmethod
    def extraire_texte_pdf(file_bytes: bytes) -> dict:
        """Extrait le texte d'un PDF texte"""
        try:
            doc = pymupdf.open(stream=file_bytes, filetype="pdf")
            
            texte_complet = ""
            pages_extraites = []
            
            for page_num, page in enumerate(doc):
                texte_page = page.get_text()
                texte_complet += f"\n--- PAGE {page_num + 1} ---\n{texte_page}"
                pages_extraites.append({
                    "page": page_num + 1,
                    "texte": texte_page,
                    "confidence": 1.0  # PDF texte = 100% confiance
                })
            
            doc.close()
            
            logger.info(f"PDF texte: {len(pages_extraites)} pages, {len(texte_complet)} caractères")
            
            return {
                "success": True,
                "method": "pdf_text",
                "texte_complet": texte_complet,
                "pages": pages_extraites,
                "confidence_moyenne": 1.0,
                "nombre_pages": len(pages_extraites)
            }
            
        except Exception as e:
            logger.error(f"Erreur extraction PDF texte: {e}")
            return {
                "success": False,
                "error": str(e)
            }

class OCRProcessor:
    """Traitement OCR pour images/scans"""
    
    def __init__(self):
        # Initialiser PaddleOCR (télécharge le modèle au premier appel)
        try:
            from paddleocr import PaddleOCR
            logger.info("Initialisation PaddleOCR...")
            self.ocr = PaddleOCR(use_gpu=False, lang='fr')  # French + English
            logger.info("✅ PaddleOCR initialisé")
        except ImportError:
            logger.error("PaddleOCR non installé")
            self.ocr = None
    
    def extraire_texte_ocr(self, file_bytes: bytes, nom_fichier: str) -> dict:
        """
        Extrait le texte d'une image via OCR
        Supporte: PNG, JPG, GIF, BMP, TIFF
        """
        if not self.ocr:
            return {
                "success": False,
                "error": "OCR non disponible"
            }
        
        try:
            from PIL import Image
            import tempfile
            
            # Charger l'image
            image = Image.open(BytesIO(file_bytes))
            
            # Sauvegarder temporairement (PaddleOCR préfère les fichiers)
            with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
                image.save(tmp.name, "PNG")
                temp_path = tmp.name
            
            # OCR
            logger.info(f"OCR en cours pour {nom_fichier}...")
            results = self.ocr.ocr(temp_path, cls=True)
            
            # Nettoyer le fichier temporaire
            Path(temp_path).unlink()
            
            # Parser les résultats
            texte_complet = ""
            elements = []
            confidences = []
            
            if results:
                for line in results:
                    if line:
                        for word_info in line:
                            text = word_info[1][0]
                            confidence = word_info[1][1]
                            
                            texte_complet += text + " "
                            confidences.append(confidence)
                            elements.append({
                                "texte": text,
                                "confidence": round(confidence, 3)
                            })
            
            confidence_moyenne = sum(confidences) / len(confidences) if confidences else 0
            
            logger.info(f"OCR complété: {len(texte_complet)} caractères, "
                       f"confiance: {confidence_moyenne:.1%}")
            
            return {
                "success": True,
                "method": "ocr_paddle",
                "texte_complet": texte_complet.strip(),
                "elements": elements,
                "confidence_moyenne": round(confidence_moyenne, 3),
                "nombre_elements": len(elements)
            }
            
        except Exception as e:
            logger.error(f"Erreur OCR: {e}")
            return {
                "success": False,
                "error": str(e)
            }

class UnifiedExtractor:
    """Pipeline unifié d'extraction"""
    
    def __init__(self):
        self.pdf_processor = PDFProcessor()
        self.ocr_processor = OCRProcessor()
    
    def extraire(self, file_bytes: bytes, nom_fichier: str, mime_type: str) -> dict:
        """
        Extrait le texte d'un fichier (PDF ou image)
        Détermine automatiquement la méthode appropriée
        """
        
        # Cas 1: Fichier PDF
        if mime_type == "application/pdf":
            if self.pdf_processor.est_pdf_texte(file_bytes):
                return self.pdf_processor.extraire_texte_pdf(file_bytes)
            else:
                # PDF scanné → OCR
                logger.info(f"PDF scanné détecté, passage à OCR...")
                # TODO: Convertir PDF scanné en images puis OCR
                return {
                    "success": False,
                    "error": "PDF scanné nécessite conversion (non implémenté)"
                }
        
        # Cas 2: Fichiers image
        elif mime_type in ["image/png", "image/jpeg", "image/tiff", "image/gif"]:
            return self.ocr_processor.extraire_texte_ocr(file_bytes, nom_fichier)
        
        # Cas 3: Format inconnu
        else:
            return {
                "success": False,
                "error": f"Format non supporté: {mime_type}"
            }

# Instance singleton
_extractor = None

def get_extractor() -> UnifiedExtractor:
    """Retourne l'instance singleton du extracteur"""
    global _extractor
    if _extractor is None:
        _extractor = UnifiedExtractor()
    return _extractor
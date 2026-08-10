"""Utilitaires OCR"""
import logging
from io import BytesIO

logger = logging.getLogger(__name__)

class PDFProcessor:
    """Traitement des fichiers PDF"""
    
    @staticmethod
    def est_pdf_texte(file_bytes: bytes) -> bool:
        """Détermine si un PDF contient du texte extractible"""
        try:
            import pdfplumber
            pdf = pdfplumber.open(BytesIO(file_bytes))
            
            if len(pdf.pages) > 0:
                first_page = pdf.pages[0]
                text = first_page.extract_text()
                pdf.close()
                return len(text.strip()) > 50 if text else False
            
            pdf.close()
            return False
            
        except Exception as e:
            logger.error(f"Erreur détection PDF: {e}")
            return False
    
    @staticmethod
    def extraire_texte_pdf(file_bytes: bytes) -> dict:
        """Extrait le texte d'un PDF texte"""
        try:
            import pdfplumber
            pdf = pdfplumber.open(BytesIO(file_bytes))
            
            texte_complet = ""
            pages_extraites = []
            
            for page_num, page in enumerate(pdf.pages):
                texte_page = page.extract_text()
                if texte_page:
                    texte_complet += f"\n--- PAGE {page_num + 1} ---\n{texte_page}"
                    pages_extraites.append({
                        "page": page_num + 1,
                        "texte": texte_page,
                        "confidence": 1.0
                    })
            
            pdf.close()
            
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
        # Try PaddleOCR first, fall back to pytesseract if available
        self.backend = None
        self.ocr = None
        try:
            from paddleocr import PaddleOCR
            logger.info("Initialisation PaddleOCR...")
            self.ocr = PaddleOCR(use_gpu=False, lang='fr')
            self.backend = 'paddle'
            logger.info("✅ PaddleOCR initialisé")
        except Exception:
            try:
                import pytesseract  # type: ignore
                logger.info("PaddleOCR non disponible, fallback vers pytesseract")
                self.backend = 'tesseract'
                # no heavy init required for pytesseract
            except Exception:
                logger.error("Aucun backend OCR installé (PaddleOCR ou pytesseract)")
                self.backend = None
                self.ocr = None
    
    def extraire_texte_ocr(self, file_bytes: bytes, nom_fichier: str) -> dict:
        """Extrait le texte d'une image via OCR"""
        from PIL import Image
        import tempfile
        from pathlib import Path

        if self.backend is None:
            return {
                "success": False,
                "error": "OCR non disponible (installer PaddleOCR ou pytesseract)"
            }

        try:
            image = Image.open(BytesIO(file_bytes))

            if self.backend == 'paddle':
                with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
                    image.save(tmp.name, "PNG")
                    temp_path = tmp.name

                logger.info(f"OCR (Paddle) en cours pour {nom_fichier}...")
                results = self.ocr.ocr(temp_path, cls=True)
                Path(temp_path).unlink()

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
                                elements.append({"texte": text, "confidence": round(confidence, 3)})

                confidence_moyenne = sum(confidences) / len(confidences) if confidences else 0

                return {
                    "success": True,
                    "method": "ocr_paddle",
                    "texte_complet": texte_complet.strip(),
                    "elements": elements,
                    "confidence_moyenne": round(confidence_moyenne, 3),
                    "nombre_elements": len(elements)
                }

            else:  # tesseract fallback
                try:
                    import pytesseract  # type: ignore
                except Exception as e:
                    logger.error(f"pytesseract import error: {e}")
                    return {"success": False, "error": "pytesseract non disponible"}

                logger.info(f"OCR (Tesseract) en cours pour {nom_fichier}...")
                # Use image_to_data to get words and confidence
                data = pytesseract.image_to_data(image, lang='fra', output_type=pytesseract.Output.DICT)
                n_boxes = len(data.get('text', []))
                texte_complet = ""
                elements = []
                confidences = []

                for i in range(n_boxes):
                    text = data['text'][i]
                    if not text or text.strip() == "":
                        continue
                    conf = float(data['conf'][i]) if data['conf'][i] != '-1' else 0.0
                    texte_complet += text + " "
                    confidences.append(conf / 100.0)
                    elements.append({"texte": text, "confidence": round(conf / 100.0, 3)})

                confidence_moyenne = sum(confidences) / len(confidences) if confidences else 0

                return {
                    "success": True,
                    "method": "ocr_tesseract",
                    "texte_complet": texte_complet.strip(),
                    "elements": elements,
                    "confidence_moyenne": round(confidence_moyenne, 3),
                    "nombre_elements": len(elements)
                }

        except Exception as e:
            logger.error(f"Erreur OCR: {e}")
            return {"success": False, "error": str(e)}

class UnifiedExtractor:
    """Pipeline unifié d'extraction"""
    
    def __init__(self):
        self.pdf_processor = PDFProcessor()
        self.ocr_processor = OCRProcessor()
    
    def extraire(self, file_bytes: bytes, nom_fichier: str, mime_type: str) -> dict:
        """Extrait le texte d'un fichier (PDF ou image)"""
        
        if mime_type == "application/pdf":
            if self.pdf_processor.est_pdf_texte(file_bytes):
                return self.pdf_processor.extraire_texte_pdf(file_bytes)
            else:
                logger.info(f"PDF scanné détecté, passage à OCR...")
                return {
                    "success": False,
                    "error": "PDF scanné nécessite conversion (non implémenté)"
                }
        
        elif mime_type in ["image/png", "image/jpeg", "image/tiff", "image/gif"]:
            return self.ocr_processor.extraire_texte_ocr(file_bytes, nom_fichier)
        
        else:
            return {
                "success": False,
                "error": f"Format non supporté: {mime_type}"
            }

_extractor = None

def get_extractor() -> UnifiedExtractor:
    """Retourne l'instance singleton du extracteur"""
    global _extractor
    if _extractor is None:
        _extractor = UnifiedExtractor()
    return _extractor
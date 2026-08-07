"""Service pour les appels Ollama"""
import requests
import logging
from app.config import get_settings
from app.utils.exceptions import OllamaException

logger = logging.getLogger(__name__)
settings = get_settings()

class OllamaService:
    """Gère la communication avec Ollama"""
    
    def __init__(self):
        self.base_url = settings.OLLAMA_BASE_URL
        self.model = settings.OLLAMA_MODEL
        self.timeout = settings.OLLAMA_TIMEOUT
    
    def verifier_disponibilite(self) -> bool:
        """Vérifie que Ollama et le modèle sont disponibles"""
        try:
            response = requests.get(
                f"{self.base_url}/api/tags",
                timeout=settings.HTTP_TIMEOUT
            )
            response.raise_for_status()
            
            models = response.json().get("models", [])
            model_names = [m["name"] for m in models]
            
            if self.model not in model_names:
                logger.warning(f"Modèle {self.model} non trouvé. Modèles: {model_names}")
                # Optionnel: télécharger le modèle automatiquement
                self._pull_model()
                return True
            
            logger.info(f"Modèle {self.model} disponible")
            return True
            
        except Exception as e:
            logger.error(f"Erreur vérification Ollama: {e}")
            raise OllamaException(f"Ollama indisponible: {str(e)}")
    
    def _pull_model(self):
        """Télécharge le modèle s'il n'existe pas"""
        try:
            logger.info(f"Téléchargement du modèle {self.model}...")
            response = requests.post(
                f"{self.base_url}/api/pull",
                json={"name": self.model},
                timeout=300  # 5 minutes
            )
            response.raise_for_status()
            logger.info(f"Modèle {self.model} téléchargé avec succès")
        except Exception as e:
            logger.error(f"Erreur pull modèle: {e}")
            raise OllamaException(f"Impossible de télécharger {self.model}: {str(e)}")
    
    def generer(self, prompt: str) -> str:
        """Appelle Ollama avec le prompt"""
        try:
            logger.info(f"Appel Ollama avec modèle {self.model}")
            
            response = requests.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model,
                    "prompt": prompt,
                    "stream": False,
                    "temperature": 0.3  # Basse température pour plus de cohérence
                },
                timeout=self.timeout
            )
            response.raise_for_status()
            
            result = response.json()["response"]
            logger.info(f"Réponse Ollama reçue ({len(result)} chars)")
            return result
            
        except requests.exceptions.Timeout:
            logger.error(f"Timeout Ollama après {self.timeout}s")
            raise OllamaException(f"Timeout Ollama (>{self.timeout}s)")
        except Exception as e:
            logger.error(f"Erreur appel Ollama: {e}")
            raise OllamaException(f"Erreur Ollama: {str(e)}")

# Instance singleton
_ollama_service = None

def get_ollama_service() -> OllamaService:
    """Retourne l'instance singleton du service Ollama"""
    global _ollama_service
    if _ollama_service is None:
        _ollama_service = OllamaService()
    return _ollama_service
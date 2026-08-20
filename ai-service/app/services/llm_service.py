"""
Service LLM - Interface avec Ollama
SPRINT 7: Support JSON parsing + prompts structurés
"""

import logging
import json
import os
import asyncio
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

# Ollama tourne sur la machine hôte (Windows), pas dans le conteneur ai-service.
# "localhost" à l'intérieur d'un conteneur Docker désigne le conteneur lui-même,
# jamais l'hôte -> il faut passer par host.docker.internal (fourni par Docker
# Desktop) pour joindre un service qui tourne sur la machine hôte.
DEFAULT_OLLAMA_URL = "http://host.docker.internal:11434"


class LLMService:
    """Interface avec Ollama/Llama3 pour prompts structurés"""

    def __init__(self, base_url: str = None):
        """
        Initialiser le service LLM

        Args:
            base_url: URL du serveur Ollama. Si non fourni, utilise la
                variable d'environnement OLLAMA_BASE_URL, sinon
                http://host.docker.internal:11434 (Ollama sur l'hôte Windows).
        """
        self.base_url = (
            base_url
            or os.getenv("OLLAMA_BASE_URL")
            or DEFAULT_OLLAMA_URL
        )
        self.model = "llama3:latest"
        self.timeout = 120  # secondes

        logger.info(f"LLMService configuré avec base_url={self.base_url}")

    async def generate(
        self,
        prompt: str,
        model: str = None,
        temperature: float = 0.3,
        max_tokens: int = 2000
    ) -> str:
        """
        Générer une réponse du LLM via Ollama

        Args:
            prompt: Prompt à envoyer
            model: Modèle à utiliser (default: llama3:latest)
            temperature: Température (0.0-1.0, default 0.3 = faible créativité)
            max_tokens: Max tokens générés

        Returns:
            Texte réponse du LLM

        Raises:
            RuntimeError: Si Ollama ne répond pas
        """

        model = model or self.model

        logger.info(f"📤 Appel LLM ({model}) - Prompt: {len(prompt)} chars")

        try:
            import httpx

            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/generate",
                    json={
                        "model": model,
                        "prompt": prompt,
                        "temperature": temperature,
                        "num_predict": max_tokens,
                        "stream": False
                    }
                )

            if response.status_code != 200:
                logger.error(f"❌ Ollama error {response.status_code}: {response.text}")
                raise RuntimeError(f"Ollama returned {response.status_code}: {response.text}")

            result = response.json()
            text = result.get("response", "").strip()

            logger.info(f"📥 LLM response: {len(text)} chars")
            return text

        except asyncio.TimeoutError:
            logger.error("❌ LLM timeout (120s) - Ollama trop lent ou down")
            raise RuntimeError("LLM timeout - Ollama not responding")

        except Exception as e:
            logger.error(f"❌ LLM error: {type(e).__name__}: {e}")
            raise RuntimeError(f"LLM error: {e}")

    async def generate_json(
        self,
        prompt: str,
        model: str = None,
        retry: int = 3
    ) -> Dict[str, Any]:
        """
        Générer et parser une réponse JSON du LLM

        Args:
            prompt: Prompt attendant JSON en réponse
            model: Modèle à utiliser
            retry: Nombre de tentatives si JSON invalide

        Returns:
            Dict parsé du JSON

        Raises:
            RuntimeError: Si JSON invalide après tentatives
        """

        prompt_actuel = prompt
        response_text = ""

        for attempt in range(1, retry + 1):
            try:
                logger.info(f"📤 Appel LLM JSON (tentative {attempt}/{retry})")

                response_text = await self.generate(prompt_actuel, model)

                # Nettoyer la réponse (supprimer backticks markdown)
                response_text = response_text.strip()
                if response_text.startswith("```json"):
                    response_text = response_text[7:]
                if response_text.startswith("```"):
                    response_text = response_text[3:]
                if response_text.endswith("```"):
                    response_text = response_text[:-3]
                response_text = response_text.strip()

                # Parser JSON
                json_data = json.loads(response_text)
                logger.info("✅ JSON parsé avec succès")
                return json_data

            except json.JSONDecodeError as e:
                logger.warning(f"⚠️  JSON invalide tentative {attempt}: {str(e)[:100]}")
                logger.debug(f"Response was: {response_text[:200]}")

                if attempt == retry:
                    logger.error(f"❌ JSON invalide après {retry} tentatives")
                    raise RuntimeError(f"LLM returned invalid JSON: {e}")

                # Retry avec prompt ajusté
                logger.info("🔄 Nouvelle tentative...")
                prompt_actuel = prompt + (
                    "\n\nREMARQUE: Répondre UNIQUEMENT en JSON valide, "
                    "sans texte avant/après."
                )
                continue

            except Exception as e:
                logger.error(f"❌ Erreur parsing JSON: {type(e).__name__}: {e}")
                raise

        raise RuntimeError("JSON parsing failed after retries")

    async def test_connection(self) -> bool:
        """
        Tester si Ollama est accessible

        Returns:
            True si OK, False sinon
        """
        try:
            logger.info(f"🔌 Test connexion Ollama ({self.base_url})")

            import httpx

            async with httpx.AsyncClient(timeout=10) as client:
                response = await client.get(f"{self.base_url}/api/tags")

            if response.status_code == 200:
                models = response.json().get("models", [])
                logger.info(f"✅ Ollama OK - {len(models)} modèles disponibles")
                return True
            else:
                logger.error(f"❌ Ollama inaccessible ({response.status_code})")
                return False

        except Exception as e:
            logger.error(f"❌ Erreur connexion Ollama: {e}")
            return False


# ============ SINGLETON ============

_llm_service = None


def get_llm_service(base_url: str = None) -> LLMService:
    """
    Retourne instance singleton du service LLM

    Args:
        base_url: URL Ollama (optionnel, utilisé une seule fois à la
            création du singleton). Si omis, utilise OLLAMA_BASE_URL
            ou http://host.docker.internal:11434 par défaut.

    Returns:
        Instance LLMService
    """
    global _llm_service
    if _llm_service is None:
        _llm_service = LLMService(base_url)
    return _llm_service
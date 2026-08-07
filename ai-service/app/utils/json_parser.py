"""Parser JSON de la réponse Ollama"""
import json
import logging
from app.utils.exceptions import JsonParsingException

logger = logging.getLogger(__name__)

def extraire_json(response_text: str) -> dict:
    """Extrait et parse le JSON de la réponse Ollama"""
    try:
        # Ollama peut retourner du texte avant/après le JSON
        start = response_text.find('{')
        end = response_text.rfind('}') + 1
        
        if start == -1 or end <= start:
            raise JsonParsingException("Pas de JSON trouvé dans la réponse")
        
        json_str = response_text[start:end]
        parsed = json.loads(json_str)
        
        logger.info(f"JSON parsé avec succès: {list(parsed.keys())}")
        return parsed
        
    except json.JSONDecodeError as e:
        logger.error(f"Erreur JSON decode: {e}, contenu: {response_text[:300]}")
        raise JsonParsingException(f"JSON invalide: {str(e)}")

def valider_analyse_response(parsed: dict) -> dict:
    """Valide que la réponse contient tous les champs requis"""
    required_fields = {
        'score_risque': (int, lambda x: 0 <= x <= 100),
        'verdict': (str, lambda x: x in ['VALIDE', 'RISQUE', 'REJETE']),
        'justification': (str, lambda x: len(x) > 0),
        'recommandations': (list, lambda x: len(x) >= 2)
    }
    
    for field, (type_expected, validator) in required_fields.items():
        if field not in parsed:
            raise JsonParsingException(f"Champ manquant: {field}")
        
        if not isinstance(parsed[field], type_expected):
            raise JsonParsingException(
                f"Champ {field}: type {type_expected.__name__} attendu, "
                f"got {type(parsed[field]).__name__}"
            )
        
        if not validator(parsed[field]):
            raise JsonParsingException(f"Champ {field}: validation échouée")
    
    return parsed
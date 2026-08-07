"""Exceptions personnalisées"""

class OllamaException(Exception):
    """Exception levée quand Ollama est indisponible"""
    pass

class AnalysisException(Exception):
    """Exception levée quand l'analyse échoue"""
    pass

class JsonParsingException(Exception):
    """Exception levée quand le parsing JSON échoue"""
    pass
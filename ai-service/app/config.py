"""Configuration centralisée de l'application"""
import os
from functools import lru_cache
from dotenv import load_dotenv

load_dotenv()

class Settings:
    """Settings de l'application"""
    
    # App
    APP_NAME: str = os.getenv("APP_NAME", "BTE AI Analysis Service")
    APP_VERSION: str = os.getenv("APP_VERSION", "1.0.0")
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    
    # Ollama
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "llama3:latest")
    
    # Timeouts
    OLLAMA_TIMEOUT: int = int(os.getenv("OLLAMA_TIMEOUT", "120"))
    HTTP_TIMEOUT: int = int(os.getenv("HTTP_TIMEOUT", "30"))
    
    # Logging
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

@lru_cache()
def get_settings() -> Settings:
    """Retourne une instance singleton de Settings"""
    return Settings()
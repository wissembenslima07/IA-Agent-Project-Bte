"""Configuration du logging structuré"""
import logging
import sys
from pythonjsonlogger import jsonlogger
from app.config import get_settings

settings = get_settings()

def setup_logging():
    """Configure le logging JSON structuré"""
    logger = logging.getLogger()
    logger.setLevel(getattr(logging, settings.LOG_LEVEL))
    
    # Handler console avec JSON
    handler = logging.StreamHandler(sys.stdout)
    formatter = jsonlogger.JsonFormatter(
        fmt="%(timestamp)s %(level)s %(name)s %(message)s"
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)
    
    return logger

logger = setup_logging()
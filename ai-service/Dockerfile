# ============================================================
# BTE - Intelligent Credit Dossier Analysis System
# FastAPI Microservice Dockerfile - ULTRA MINIMAL
# ============================================================

FROM python:3.10

WORKDIR /app

# ============ UPGRADE PIP SEULEMENT ============
RUN python -m pip install --no-cache-dir --upgrade pip setuptools wheel

# ============ INSTALLER LES DÉPENDANCES PYTHON ============
COPY requirements.txt .
RUN pip install --no-cache-dir --prefer-binary --default-timeout=1000 --retries=10 -r requirements.txt

# ============ COPIER LE CODE ============
COPY app/ app/
COPY .env .

# ============ HEALTH CHECK ============
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8000/health', timeout=5)" || exit 1

# ============ EXPOSER LE PORT ============
EXPOSE 8000

# ============ DÉMARRER L'APPLICATION ============
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
# BTE AI Analysis Service

Microservice FastAPI pour l'analyse IA des dossiers de crédit avec Ollama + Llama 3.

Ce service est prévu pour fonctionner dans Docker, mais il appelle un Ollama installé sur la machine hôte via `host.docker.internal:11434`.

## Installation

```bash
# Créer l'environnement virtuel
python -m venv venv

# Activer
# Windows:
venv\Scripts\activate
# Unix:
source venv/bin/activate

# Installer les dépendances
pip install -r requirements.txt
```

## Configuration

Copie `.env.example` vers `.env` et remplis les variables :

```bash
cp .env.example .env
```

## Démarrage local

```bash
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Puis visite `http://localhost:8000/docs` pour l'API interactive.

## Docker

```bash
docker build -t bte-ai-service .
docker run -p 8000:8000 -e OLLAMA_BASE_URL=http://host.docker.internal:11434 bte-ai-service
```

Si tu exécutes le service hors Docker sur Windows, garde `OLLAMA_BASE_URL=http://localhost:11434`.

## Endpoints

- `GET /health` — État de santé
- `POST /api/analyse` — Lancer une analyse

## Tests

```bash
curl -X POST http://localhost:8000/api/analyse \
  -H "Content-Type: application/json" \
  -d '{...}'
```
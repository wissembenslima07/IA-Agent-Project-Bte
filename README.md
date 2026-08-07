# BTE Credit Risk Management System

## Overview

The **BTE Credit Risk Management System** is a web-based application developed as part of a summer internship at **Banque de Tunisie et des Émirats (BTE)**.

The application helps banking advisors and administrators manage credit applications, assess customer risk, and monitor the entire credit approval process. It also integrates an AI service to assist in credit analysis.

---

## Architecture

The project follows a **microservices architecture** composed of four main services.

```
                +-------------------+
                |     Frontend      |
                |      Angular      |
                +---------+---------+
                          |
                          |
                REST APIs |
                          |
        +-----------------+------------------+
        |                                    |
        |                                    |
+-------+--------+                  +--------+-------+
|   Spring Boot  |                  |   AI Service   |
|    Backend     |                  |    FastAPI     |
+-------+--------+                  +--------+-------+
        |                                    |
        |                                    |
        +-----------------+------------------+
                          |
                    PostgreSQL Database
```

---

# Technologies

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Maven
- JWT Authentication
- OpenFeign

---

## Frontend

- Angular
- TypeScript
- Tailwind CSS
- HTML5
- RxJS

---

## AI Service

- Python
- FastAPI
- Ollama
- LangChain
- LangGraph

In Docker, the AI service calls an Ollama instance running on the host machine, not in a dedicated container.

---

## Database

- PostgreSQL 16

---

## DevOps

- Docker
- Docker Compose
- Git
- GitHub
- GitHub Actions (CI)

---

# Project Structure

```
BTE-project
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── ai-service/
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
│
├── db/
│   └── init.sql
│
├── docker-compose.yml
│
└── README.md
```

---

# Features

## Authentication

- JWT Authentication
- Login
- Logout
- Role-Based Access Control

Roles

- Administrator
- Credit Advisor

---

## Credit File Management

- Create credit application
- Update application
- View application details
- Search applications
- Filter applications
- Credit history

---

## Dashboard

- Statistics
- Credit portfolio overview
- Recent applications
- Risk alerts
- AI summary

---

## AI Analysis

- Credit risk prediction
- AI recommendations
- Global portfolio analysis

---

## Audit

- User activity history
- Credit file history
- Action tracking

---

# Database

Main tables

- utilisateurs
- dossiers_credit
- historique_actions

---

# Running the Project

## Requirements

Install

- Java 21
- Maven
- Node.js 22+
- Python 3.11+
- Docker Desktop
- Docker Compose

---

## Clone the project

```bash
git clone https://github.com/yourusername/BTE-project.git

cd BTE-project
```

---

## Run with Docker

```bash
docker compose up --build
```

Pre-requisite: start Ollama locally on your Windows machine before launching Docker.

Services

| Service | Port |
|----------|------|
| Frontend | 4200 |
| Backend | 8080 |
| AI Service | 8000 |
| PostgreSQL | 5432 |

---

## Backend

Run locally

```bash
cd backend

./mvnw spring-boot:run
```

Windows

```bash
mvnw.cmd spring-boot:run
```

---

## Frontend

```bash
cd frontend

npm install

ng serve
```

Open

```
http://localhost:4200
```

---

## AI Service

```bash
cd ai-service

python -m venv venv

source venv/bin/activate
```

Windows

```bash
venv\Scripts\activate
```

Install dependencies

```bash
pip install -r requirements.txt
```

Run

```bash
python run.py
```

---

# Docker

Start

```bash
docker compose up --build
```

Stop

```bash
docker compose down
```

Remove volumes

```bash
docker compose down -v
```

---

# Continuous Integration

GitHub Actions automatically:

- Builds the backend
- Builds the frontend
- Detects compilation errors
- Validates every Push and Pull Request

Workflow location

```
.github/workflows/ci.yml
```

---

# Security

- JWT Authentication
- BCrypt Password Encryption
- Spring Security
- Role-Based Authorization
- CORS Configuration

---

# Future Improvements

- OCR document extraction
- Email notifications
- AI chatbot
- Credit score visualization
- Report generation
- File upload
- PDF generation
- Kafka integration
- Monitoring with Prometheus & Grafana

---

# Authors

**Wissem Ben Slima**

Software Engineering Student

Backend & Microservices Developer

Summer Internship – Banque de Tunisie et des Émirats (BTE)

---

# License

This project was developed for educational and internship purposes.

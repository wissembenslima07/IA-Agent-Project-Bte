"""
Schémas Pydantic pour analyse de crédit multi-fiches
SPRINT 7: Structures pour verdicts et analyses
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


# ============ FICHE DE PAIE INDIVIDUELLE ============

class FicheDePayeAnalysis(BaseModel):
    """Analyse d'UNE seule fiche de paie"""

    mois: str = Field(..., description="Mois de la fiche (ex: 'Janvier 2024')")

    # Montants extraits
    salaire_brut: float = Field(..., ge=0, description="Salaire brut en euros")
    salaire_net: float = Field(..., ge=0, description="Salaire net en euros")
    charges_sociales: float = Field(default=0, ge=0, description="Charges sociales")
    impots: float = Field(default=0, ge=0, description="Impôts")
    retenues: float = Field(default=0, ge=0, description="Autres retenues")
    bonus: float = Field(default=0, ge=0, description="Bonus (si présent)")
    primes: float = Field(default=0, ge=0, description="Primes (si présentes)")

    # Analyse
    stabilite_salaire: str = Field(..., description="stable|variable|bonus_dependent")
    revenus_fiables: float = Field(..., ge=0, description="Revenus garantis (salaire base)")
    revenus_complementaires: float = Field(..., ge=0, description="Revenus non-garantis (bonus+primes)")

    # Notes
    notes: str = Field(default="", description="Observations brèves")

    class Config:
        json_schema_extra = {
            "example": {
                "mois": "Janvier 2024",
                "salaire_brut": 3000,
                "salaire_net": 2400,
                "bonus": 500,
                "stabilite_salaire": "stable",
                "revenus_fiables": 3000,
                "revenus_complementaires": 500
            }
        }


# ============ ANALYSE COMPARATIVE (plusieurs fiches) ============

class AnalyseComparative(BaseModel):
    """Résultat de la comparaison des fiches de paie"""

    periode: str = Field(..., description="Période couverte (ex: 'Janvier-Mars 2024')")
    nombre_fiches: int = Field(default=3, description="Nombre de fiches analysées")

    # Statistiques
    salaire_moyen: float = Field(..., ge=0, description="Salaire moyen")
    salaire_min: float = Field(..., ge=0, description="Salaire minimum")
    salaire_max: float = Field(..., ge=0, description="Salaire maximum")
    salaire_std_dev: Optional[float] = Field(default=None, ge=0, description="Écart-type")

    # Tendances
    tendance: str = Field(..., description="hausse|baisse|stable")
    volatilite: float = Field(..., ge=0, le=100, description="Volatilité (0-100)")

    # Revenus
    revenus_garantis_moyen: float = Field(..., ge=0, description="Revenus garantis moyens")
    revenus_variables_moyen: float = Field(..., ge=0, description="Revenus variables moyens")
    ratio_variables_sur_total: float = Field(..., ge=0, le=1, description="Ratio 0-1")

    # Évaluation
    capacite_remboursement: str = Field(
        ..., description="Excellente|Bonne|Moyenne|Faible"
    )
    observations: List[str] = Field(..., description="Observations clés")

    class Config:
        json_schema_extra = {
            "example": {
                "periode": "Janvier-Mars 2024",
                "salaire_moyen": 2450,
                "tendance": "stable",
                "volatilite": 15,
                "capacite_remboursement": "Bonne",
                "observations": ["Salaire stable", "Revenus variables minimes"]
            }
        }


# ============ VERDICT FINAL ============

class VerdictFinal(BaseModel):
    """Verdict de crédit final"""

    # Core
    score_risque: int = Field(..., ge=0, le=100, description="Score de risque 0-100")
    verdict: str = Field(..., description="VALIDE|RISQUE|REJETE")
    confiance: float = Field(..., ge=0.0, le=1.0, description="Confiance 0.0-1.0")

    # Détails
    points_forts: List[str] = Field(..., description="Points positifs")
    risques_majeurs: List[str] = Field(..., description="Risques identifiés")
    tendances_observees: List[str] = Field(..., description="Tendances observées")

    # Recommandations
    montant_max_recommande: Optional[float] = Field(
        default=None,
        ge=0,
        description="Montant max en euros"
    )
    duree_max_recommandee: Optional[str] = Field(
        default=None,
        description="12 mois|24 mois|36 mois|48 mois"
    )
    conditions_speciales: Optional[List[str]] = Field(
        default=None,
        description="Conditions particulières"
    )
    taux_interet_recommande: Optional[str] = Field(
        default=None,
        description="Normal|Majoré 0.25%|Majoré 0.5%"
    )

    # Justification
    justification: str = Field(..., description="Justification 300-500 caractères")
    resume_court: str = Field(..., description="Résumé 1-2 lignes")

    class Config:
        json_schema_extra = {
            "example": {
                "score_risque": 25,
                "verdict": "VALIDE",
                "confiance": 0.92,
                "montant_max_recommande": 50000,
                "duree_max_recommandee": "24 mois",
                "justification": "Candidat avec revenus stables et endettement faible."
            }
        }


# ============ ANALYSE COMPLÈTE (Pipeline) ============

class AnalyseCompleteFiches(BaseModel):
    """Résultat complet du pipeline d'analyse multi-fiches"""

    # Metadata
    dossier_id: int = Field(..., description="ID du dossier de crédit")
    numero_fiches_analysees: int = Field(..., ge=2, le=12, description="Nombre fiches")
    periode: str = Field(..., description="Période couverte")
    timestamp: datetime = Field(default_factory=datetime.now, description="Date d'analyse")

    # Étapes du pipeline
    fiches_individuelles: List[FicheDePayeAnalysis] = Field(..., description="Analyses individuelles")
    analyse_comparative: AnalyseComparative = Field(..., description="Analyse comparative")
    verdict_final: VerdictFinal = Field(..., description="Verdict final")

    # Qualité
    overall_confidence: float = Field(..., ge=0.0, le=1.0, description="Confiance globale")
    notes_analystes: Optional[str] = Field(default=None, description="Notes supplémentaires")

    class Config:
        json_schema_extra = {
            "example": {
                "dossier_id": 1,
                "numero_fiches_analysees": 3,
                "periode": "Janvier-Mars 2024",
                "fiches_individuelles": [],
                "analyse_comparative": {},
                "verdict_final": {},
                "overall_confidence": 0.92
            }
        }


# ============ POUR LA BASE DE DONNÉES ============

class VerdictCreditDB(BaseModel):
    """Données à sauvegarder en BD (verdicts_credit)"""

    dossier_id: int
    nombreFichesAnalysees: int
    periode: str
    # NOTE: cette classe semble incomplète dans le fichier original
    # (coupée après "periode: str"). Ajouter ici les champs manquants
    # si d'autres colonnes de la table verdicts_credit sont nécessaires
    # (ex: score_risque, verdict, timestamp...).
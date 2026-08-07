"""Schemas Pydantic pour l'analyse"""
from typing import List, Optional

from pydantic import BaseModel, Field


class DocumentForAnalysis(BaseModel):
    """Document a analyser"""

    typeDocument: str = Field(..., description="Type du document (ex: FICHE_PAIE)")
    contenu: str = Field(..., description="Contenu/resume du document")

    class Config:
        json_schema_extra = {
            "example": {
                "typeDocument": "FICHE_PAIE",
                "contenu": "Salaire mensuel 3500 EUR, emploi depuis 2 ans",
            }
        }


class AnalyseRequest(BaseModel):
    """Demande d'analyse"""

    dossierId: int = Field(..., description="ID du dossier credit")
    clientNom: str = Field(..., description="Nom du client")
    clientPrenom: str = Field(..., description="Prenom du client")
    documents: List[DocumentForAnalysis] = Field(..., description="Documents a analyser")
    contexteSupplementaire: Optional[str] = Field(
        None,
        description="Contexte supplementaire (emploi, secteur, etc.)",
    )

    class Config:
        json_schema_extra = {
            "example": {
                "dossierId": 1,
                "clientNom": "Dupont",
                "clientPrenom": "Jean",
                "documents": [
                    {
                        "typeDocument": "FICHE_PAIE",
                        "contenu": "Salaire 3500 EUR",
                    }
                ],
                "contexteSupplementaire": "Demande credit immobilier 200k EUR",
            }
        }
"""Schemas Pydantic pour l'analyse"""
from pydantic import BaseModel, Field
from typing import Optional, List

class DocumentForAnalysis(BaseModel):
    """Document à analyser"""
    typeDocument: str = Field(..., description="Type du document (ex: FICHE_PAIE)")
    contenu: str = Field(..., description="Contenu/résumé du document")
    
    class Config:
        json_schema_extra = {
            "example": {
                "typeDocument": "FICHE_PAIE",
                "contenu": "Salaire mensuel 3500 EUR, emploi depuis 2 ans"
            }
        }

class AnalyseRequest(BaseModel):
    """Demande d'analyse"""
    dossierId: int = Field(..., description="ID du dossier crédit")
    clientNom: str = Field(..., description="Nom du client")
    clientPrenom: str = Field(..., description="Prénom du client")
    documents: List[DocumentForAnalysis] = Field(..., description="Documents à analyser")
    contexteSupplementaire: Optional[str] = Field(
        None, 
        description="Contexte supplémentaire (emploi, secteur, etc.)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "dossierId": 1,
                "clientNom": "Dupont",
                "clientPrenom": "Jean",
                "documents": [
                    {
                        "typeDocument": "FICHE_PAIE",
                        "contenu": "Salaire 3500 EUR"
                    }
                ],
                "contexteSupplementaire": "Demande crédit immobilier 200k EUR"
            }
        }
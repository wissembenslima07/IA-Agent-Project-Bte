"""
Service d'analyse de crédit multi-fiches
SPRINT 7: Pipeline 3-étapes avec Ollama/Llama3
"""

import logging
import json
from typing import List
from datetime import datetime

from app.schemas.credit import (
    FicheDePayeAnalysis,
    AnalyseComparative,
    VerdictFinal,
    AnalyseCompleteFiches
)
from app.services.prompt_service import get_prompt_service
from app.services.llm_service import get_llm_service

logger = logging.getLogger(__name__)


class CreditAnalysisService:
    """
    Service d'analyse de crédit - Pipeline 3 étapes

    ÉTAPE 1: Analyser chaque fiche individuellement (LLM)
    ÉTAPE 2: Comparer et extraire tendances (LLM)
    ÉTAPE 3: Générer verdict final (LLM)

    Temps total: ~10-15 secondes (3 appels LLM)
    """

    def __init__(self, prompt_service=None, llm_service=None):
        """
        Initialiser le service

        Args:
            prompt_service: Service de construction de prompts
            llm_service: Service LLM (Ollama)
        """
        self.prompts = prompt_service or get_prompt_service()
        self.llm = llm_service or get_llm_service()
        self.model = "llama3:latest"

    # ============ ÉTAPE 1: Analyser fiche individuelle ============

    async def analyze_single_fiche(
        self,
        fiche_text: str,
        mois: str
    ) -> FicheDePayeAnalysis:
        """
        Analyser UNE fiche de paie

        Args:
            fiche_text: Texte extrait par OCR
            mois: Mois de la fiche

        Returns:
            FicheDePayeAnalysis structuré

        Raises:
            RuntimeError: Si LLM ou parsing échoue
        """

        logger.info(f"📋 Analyse fiche: {mois}")

        try:
            # Construire prompt
            prompt = self.prompts.prompt_analyser_fiche_individuelle(mois, fiche_text)

            # Appeler LLM
            response_json = await self.llm.generate_json(prompt, model=self.model)

            # Parser et valider
            analysis = FicheDePayeAnalysis(**response_json)
            logger.info(f"✅ Fiche {mois}: {analysis.salaire_net}€")
            return analysis

        except Exception as e:
            logger.error(f"❌ Erreur analyse {mois}: {e}")
            raise

    # ============ ÉTAPE 2: Comparer les fiches ============

    async def compare_fiches(
        self,
        analyses: List[FicheDePayeAnalysis]
    ) -> AnalyseComparative:
        """
        Comparer les fiches et extraire tendances

        Args:
            analyses: Liste des fiches analysées

        Returns:
            AnalyseComparative avec tendances
        """

        logger.info(f"📊 Comparaison de {len(analyses)} fiches")

        try:
            # Convertir en JSON pour LLM
            analyses_json = json.dumps(
                [a.model_dump() for a in analyses],
                indent=2,
                ensure_ascii=False
            )

            # Construire prompt
            prompt = self.prompts.prompt_comparer_fiches(analyses_json)

            # Appeler LLM
            response_json = await self.llm.generate_json(prompt, model=self.model)

            # Parser et valider
            comparison = AnalyseComparative(**response_json)
            logger.info(f"✅ Comparaison: Tendance={comparison.tendance}, "
                       f"Volatilité={comparison.volatilite}%")
            return comparison

        except Exception as e:
            logger.error(f"❌ Erreur comparaison: {e}")
            raise

    # ============ ÉTAPE 3: Générer verdict final ============

    async def generate_verdict(
        self,
        analyses: List[FicheDePayeAnalysis],
        comparaison: AnalyseComparative,
        dossier_info: dict = None
    ) -> VerdictFinal:
        """
        Générer le verdict final de crédit

        Args:
            analyses: Fiches individuelles
            comparaison: Analyse comparative
            dossier_info: Infos supplémentaires

        Returns:
            VerdictFinal avec score et recommandations
        """

        logger.info("✅ Génération verdict final...")

        try:
            dossier_info = dossier_info or {}

            # Convertir en JSON pour LLM
            analyses_json = json.dumps(
                [a.model_dump() for a in analyses],
                indent=2,
                ensure_ascii=False
            )
            comparaison_json = json.dumps(
                comparaison.model_dump(),
                indent=2,
                ensure_ascii=False
            )

            # Construire prompt
            prompt = self.prompts.prompt_generer_verdict_final(
                analyses_json,
                comparaison_json,
                dossier_info
            )

            # Appeler LLM
            response_json = await self.llm.generate_json(prompt, model=self.model)

            # Parser et valider
            verdict = VerdictFinal(**response_json)
            logger.info(f"✅ Verdict: {verdict.verdict} (Score: {verdict.score_risque})")
            return verdict

        except Exception as e:
            logger.error(f"❌ Erreur verdict: {e}")
            raise

    # ============ ORCHESTRATION: PIPELINE COMPLET ============

    async def analyze_multiple_fiches(
        self,
        documents: List[dict],
        dossier_id: int,
        dossier_info: dict = None
    ) -> AnalyseCompleteFiches:
        """
        Pipeline COMPLET: N fiches -> Verdict final

        Args:
            documents: [{"texte": "...", "mois": "Janvier 2024"}, ...]
            dossier_id: ID du dossier
            dossier_info: Infos additionnelles

        Returns:
            AnalyseCompleteFiches complet

        Temps: ~10-15 secondes
        """
        logger.info(f"🚀 Analyse complète {len(documents)} fiches (Dossier {dossier_id})")

        dossier_info = dossier_info or {}

        # ÉTAPE 1: Analyser chaque fiche
        logger.info("📋 ÉTAPE 1: Analyses individuelles...")
        analyses = []

        for doc in documents:
            try:
                analysis = await self.analyze_single_fiche(
                    fiche_text=doc["texte"],
                    mois=doc["mois"]
                )
                analyses.append(analysis)
            except Exception as e:
                logger.error(f"❌ Erreur fiche {doc['mois']}: {e}")
                raise

        logger.info(f"✅ {len(analyses)} fiches analysées")

        # ÉTAPE 2: Comparer
        logger.info("📊 ÉTAPE 2: Analyse comparative...")
        try:
            comparaison = await self.compare_fiches(analyses)
        except Exception as e:
            logger.error(f"❌ Erreur comparaison: {e}")
            raise

        logger.info("✅ Comparaison complète")

        # ÉTAPE 3: Verdict final
        logger.info("✅ ÉTAPE 3: Verdict final...")
        try:
            verdict = await self.generate_verdict(
                analyses=analyses,
                comparaison=comparaison,
                dossier_info=dossier_info
            )
        except Exception as e:
            logger.error(f"❌ Erreur verdict: {e}")
            raise

        # Assembler résultat complet
        resultat = AnalyseCompleteFiches(
            dossier_id=dossier_id,
            numero_fiches_analysees=len(documents),
            periode=f"{documents[0]['mois']}-{documents[-1]['mois']}",
            timestamp=datetime.now(),
            fiches_individuelles=analyses,
            analyse_comparative=comparaison,
            verdict_final=verdict,
            overall_confidence=verdict.confiance
        )

        logger.info(f"✅ ANALYSE COMPLÈTE: {verdict.verdict} (Confiance: {verdict.confiance})")
        return resultat


# ============ SINGLETON ============

_credit_service = None


def get_credit_service(
    prompt_service=None,
    llm_service=None
) -> CreditAnalysisService:
    """
    Retourne instance singleton du service

    Args:
        prompt_service: Service prompts (optionnel)
        llm_service: Service LLM (optionnel)

    Returns:
        Instance CreditAnalysisService
    """
    global _credit_service
    if _credit_service is None:
        _credit_service = CreditAnalysisService(prompt_service, llm_service)
    return _credit_service
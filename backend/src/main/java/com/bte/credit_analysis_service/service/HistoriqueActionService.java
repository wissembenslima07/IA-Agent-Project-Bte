package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.model.DossierCredit;
import com.bte.credit_analysis_service.model.HistoriqueAction;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.HistoriqueActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoriqueActionService {

    private final HistoriqueActionRepository historiqueActionRepository;

    public void enregistrer(DossierCredit dossier, Utilisateur utilisateur, String action, String details) {
        HistoriqueAction entry = new HistoriqueAction();
        entry.setDossier(dossier);
        entry.setUtilisateur(utilisateur);
        entry.setAction(action);
        entry.setDetails(details);
        historiqueActionRepository.save(entry);
    }
}
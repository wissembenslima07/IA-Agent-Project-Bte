package com.bte.credit_analysis_service.service;

import com.bte.credit_analysis_service.dto.DossierCreateRequest;
import com.bte.credit_analysis_service.dto.DossierResponse;
import com.bte.credit_analysis_service.dto.HistoriqueActionResponse;
import com.bte.credit_analysis_service.dto.StatutUpdateRequest;
import com.bte.credit_analysis_service.model.DossierCredit;
import com.bte.credit_analysis_service.model.Utilisateur;
import com.bte.credit_analysis_service.repository.DossierCreditRepository;
import com.bte.credit_analysis_service.repository.HistoriqueActionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DossierCreditService {

    private final DossierCreditRepository dossierCreditRepository;
    private final HistoriqueActionRepository historiqueActionRepository;
    private final HistoriqueActionService historiqueActionService;

    public DossierResponse creerDossier(DossierCreateRequest request, Utilisateur conseiller) {
        DossierCredit dossier = new DossierCredit();
        dossier.setClientNom(request.clientNom());
        dossier.setClientPrenom(request.clientPrenom());
        dossier.setConseiller(conseiller);

        DossierCredit saved = dossierCreditRepository.save(dossier);

        historiqueActionService.enregistrer(saved, conseiller, "CREATION_DOSSIER",
            "Dossier cree pour le client " + request.clientNom() + " " + request.clientPrenom());

        return toResponse(saved);
    }

    public Page<DossierResponse> listerDossiersDuConseiller(Long conseillerId, Pageable pageable) {
        return dossierCreditRepository.findByConseillerId(conseillerId, pageable)
            .map(this::toResponse);
    }

    public Page<DossierResponse> listerDossiers(Utilisateur utilisateur, Pageable pageable) {
        if (utilisateur != null && utilisateur.getRole() == com.bte.credit_analysis_service.model.Role.ADMIN) {
            return dossierCreditRepository.findAll(pageable).map(this::toResponse);
        }
        // Par défaut, on filtre par conseiller
        Long conseillerId = utilisateur != null ? utilisateur.getId() : null;
        if (conseillerId == null) {
            // Aucun utilisateur authentifié: renvoyer page vide
            return Page.<DossierCredit>empty(pageable).map(this::toResponse);
        }
        return listerDossiersDuConseiller(conseillerId, pageable);
    }

    public DossierResponse consulterDossier(Long dossierId) {
        DossierCredit dossier = dossierCreditRepository.findById(dossierId)
            .orElseThrow(() -> new EntityNotFoundException("Dossier introuvable : " + dossierId));
        return toResponse(dossier);
    }

    public DossierResponse mettreAJourStatut(Long dossierId, StatutUpdateRequest request, Utilisateur acteur) {
        DossierCredit dossier = dossierCreditRepository.findById(dossierId)
            .orElseThrow(() -> new EntityNotFoundException("Dossier introuvable : " + dossierId));

        var ancienStatut = dossier.getStatut();
        dossier.setStatut(request.statut());
        DossierCredit updated = dossierCreditRepository.save(dossier);

        historiqueActionService.enregistrer(updated, acteur, "CHANGEMENT_STATUT",
            "Statut modifie de " + ancienStatut + " vers " + request.statut());

        return toResponse(updated);
    }

    public java.util.List<HistoriqueActionResponse> consulterHistorique(Long dossierId) {
        return historiqueActionRepository.findByDossierIdOrderByDateActionDesc(dossierId)
            .stream()
            .map(h -> new HistoriqueActionResponse(
                h.getId(),
                h.getAction(),
                h.getDetails(),
                h.getUtilisateur() != null ? h.getUtilisateur().getEmail() : "SYSTEME",
                h.getDateAction()
            ))
            .toList();
    }

    private DossierResponse toResponse(DossierCredit dossier) {
        return new DossierResponse(
            dossier.getId(),
            dossier.getClientNom(),
            dossier.getClientPrenom(),
            dossier.getConseiller() != null ? dossier.getConseiller().getEmail() : null,
            dossier.getStatut(),
            dossier.getDateCreation()
        );
    }
}
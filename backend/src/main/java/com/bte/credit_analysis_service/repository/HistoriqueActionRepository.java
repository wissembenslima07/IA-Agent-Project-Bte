package com.bte.credit_analysis_service.repository;

import com.bte.credit_analysis_service.model.HistoriqueAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueActionRepository extends JpaRepository<HistoriqueAction, Long> {
    List<HistoriqueAction> findByDossierIdOrderByDateActionDesc(Long dossierId);
}
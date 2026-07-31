package com.bte.credit_analysis_service.repository;

import com.bte.credit_analysis_service.model.DossierCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DossierCreditRepository extends JpaRepository<DossierCredit, Long> {
    Page<DossierCredit> findByConseillerId(Long conseillerId, Pageable pageable);
}
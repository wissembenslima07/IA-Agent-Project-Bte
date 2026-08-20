package com.bte.credit_analysis_service.repository;

import com.bte.credit_analysis_service.model.VerdictCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerdictRepository extends JpaRepository<VerdictCredit, Long> {
    List<VerdictCredit> findByDossierId(Long dossierId);
    List<VerdictCredit> findByVerdict(String verdict);
}
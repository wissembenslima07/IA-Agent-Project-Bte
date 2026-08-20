package com.bte.credit_analysis_service.repository;

import com.bte.credit_analysis_service.model.FicheAnalysee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FicheAnalyseeRepository extends JpaRepository<FicheAnalysee, Long> {
    List<FicheAnalysee> findByVerdictId(Long verdictId);
}
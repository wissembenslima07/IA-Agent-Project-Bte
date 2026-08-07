package com.bte.credit_analysis_service.repository;

import com.bte.credit_analysis_service.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByDossierId(Long dossierId);

    Optional<Document> findByIdAndDossierId(Long id, Long dossierId);
}
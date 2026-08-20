package com.bte.credit_analysis_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fiches_analysees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FicheAnalysee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "verdict_id", nullable = false)
    private Long verdictId;
    
    @Column(name = "mois", nullable = false)
    private String mois;
    
    @Column(name = "salaireBrut", nullable = false)
    private BigDecimal salaireBrut;
    
    @Column(name = "salaireNet", nullable = false)
    private BigDecimal salaireNet;
    
    @Column(name = "charges")
    private BigDecimal charges;
    
    @Column(name = "impots")
    private BigDecimal impots;
    
    @Column(name = "bonus")
    private BigDecimal bonus;
    
    @Column(name = "primes")
    private BigDecimal primes;
    
    @Column(name = "stabiliteSalaire")
    private String stabiliteSalaire;
    
    @Column(name = "revenus_fiables")
    private BigDecimal revenus_fiables;
    
    @Column(name = "revenusComplementaires")
    private BigDecimal revenusComplementaires;
    
    @Column(name = "date_analyse", nullable = false)
    private LocalDateTime dateAnalyse;
}
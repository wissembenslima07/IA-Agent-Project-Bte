-- ============================================================
-- BTE CREDIT ANALYSIS SYSTEM - DATABASE INITIALIZATION
-- Complete Schema: Sprint 1-7
-- ============================================================

-- Nettoyage pour permettre un rerun propre sur une base de dev
DROP TABLE IF EXISTS fiches_analysees CASCADE;
DROP TABLE IF EXISTS verdicts_credit CASCADE;
DROP TABLE IF EXISTS donnees_extraites CASCADE;
DROP TABLE IF EXISTS historique_actions CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS dossiers_credit CASCADE;
DROP TABLE IF EXISTS utilisateurs CASCADE;

-- ============================================================
-- SPRINT 1-5: TABLES BACKEND (Spring Boot)
-- ============================================================

CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CONSEILLER',
    date_creation TIMESTAMP DEFAULT NOW()
);

CREATE TABLE dossiers_credit (
    id BIGSERIAL PRIMARY KEY,
    client_nom VARCHAR(150) NOT NULL,
    client_prenom VARCHAR(150) NOT NULL,
    conseiller_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(30) DEFAULT 'EN_COURS',
    date_creation TIMESTAMP DEFAULT NOW()
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    ressource VARCHAR(100) NOT NULL
);

CREATE TABLE historique_actions (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    action VARCHAR(100) NOT NULL,
    details VARCHAR(500),
    date_action TIMESTAMP DEFAULT NOW()
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,
    type_document VARCHAR(100) NOT NULL,
    nom_fichier VARCHAR(500) NOT NULL,
    chemin_fichier VARCHAR(500) NOT NULL UNIQUE,
    taille_bytes BIGINT,
    mime_type VARCHAR(100),
    date_upload TIMESTAMP DEFAULT NOW(),
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE'
);

-- ============================================================
-- SPRINT 6: OCR EXTRACTION TABLE
-- ============================================================

CREATE TABLE donnees_extraites (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,
    text_complet TEXT NOT NULL,
    methode VARCHAR(50),
    confidence_moyenne DOUBLE PRECISION,
    nombre_pages INTEGER,
    nombre_elements INTEGER,
    date_extraction TIMESTAMP DEFAULT NOW(),
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(50) DEFAULT 'VALIDE'
);

CREATE INDEX idx_donnees_document ON donnees_extraites(document_id);
CREATE INDEX idx_donnees_dossier ON donnees_extraites(dossier_id);
CREATE INDEX idx_donnees_confidence ON donnees_extraites(confidence_moyenne);

CREATE INDEX idx_documents_dossier ON documents(dossier_id);
CREATE INDEX idx_documents_statut ON documents(statut);

CREATE INDEX idx_historique_dossier ON historique_actions(dossier_id);
CREATE INDEX idx_dossiers_conseiller ON dossiers_credit(conseiller_id);

-- ============================================================
-- SPRINT 7: ANALYSIS & VERDICTS TABLES
-- Colonnes en snake_case pour correspondre au mapping Hibernate
-- (naming strategy par defaut de Spring Boot: CamelCase -> snake_case)
-- ============================================================

CREATE TABLE verdicts_credit (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,

    -- Metadata
    nombre_fiches_analysees INTEGER NOT NULL,
    periode VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW() NOT NULL,

    -- Resultats principaux
    score_risque INTEGER NOT NULL CHECK (score_risque >= 0 AND score_risque <= 100),
    verdict VARCHAR(50) NOT NULL CHECK (verdict IN ('VALIDE', 'RISQUE', 'REJETE')),
    confiance DOUBLE PRECISION NOT NULL CHECK (confiance >= 0.0 AND confiance <= 1.0),

    -- Details JSON (stockes en TEXT)
    points_forts TEXT,
    risques_majeurs TEXT,
    tendances_observees TEXT,

    -- Recommandations
    montant_max_recommande DECIMAL(12,2),
    duree_max_recommandee VARCHAR(50),
    conditions_speciales TEXT,
    taux_interet_recommande VARCHAR(50),

    -- Justification
    justification TEXT NOT NULL,
    resume_court VARCHAR(255) NOT NULL,

    -- Audit
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(50) DEFAULT 'VALIDE' CHECK (statut IN ('VALIDE', 'EN_ATTENTE_VALIDATION', 'REJETE')),
    commentaire_validation TEXT
);

CREATE TABLE fiches_analysees (
    id BIGSERIAL PRIMARY KEY,
    verdict_id BIGINT NOT NULL REFERENCES verdicts_credit(id) ON DELETE CASCADE,

    mois VARCHAR(50) NOT NULL,

    salaire_brut DECIMAL(12,2) NOT NULL,
    salaire_net DECIMAL(12,2) NOT NULL,
    charges DECIMAL(12,2) DEFAULT 0,
    impots DECIMAL(12,2) DEFAULT 0,
    bonus DECIMAL(12,2) DEFAULT 0,
    primes DECIMAL(12,2) DEFAULT 0,

    stabilite_salaire VARCHAR(50),
    revenus_fiables DECIMAL(12,2),
    revenus_complementaires DECIMAL(12,2),

    date_analyse TIMESTAMP DEFAULT NOW() NOT NULL
);

-- ============================================================
-- SPRINT 7 INDEXES
-- ============================================================

CREATE INDEX idx_verdicts_dossier ON verdicts_credit(dossier_id);
CREATE INDEX idx_verdicts_verdict ON verdicts_credit(verdict);
CREATE INDEX idx_verdicts_score ON verdicts_credit(score_risque);
CREATE INDEX idx_verdicts_timestamp ON verdicts_credit(timestamp);
CREATE INDEX idx_verdicts_utilisateur ON verdicts_credit(utilisateur_id);
CREATE INDEX idx_verdicts_statut ON verdicts_credit(statut);

CREATE INDEX idx_fiches_verdict ON fiches_analysees(verdict_id);
CREATE INDEX idx_fiches_mois ON fiches_analysees(mois);
CREATE INDEX idx_fiches_salaire ON fiches_analysees(salaire_net);

-- ============================================================
-- DATA INSERTION (Test Data)
-- ============================================================

INSERT INTO permissions (role, action, ressource) VALUES
('ADMIN', 'MANAGE', 'UTILISATEURS'),
('CONSEILLER', 'CREATE', 'DOSSIER'),
('CONSEILLER', 'READ', 'DOSSIER');

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role) VALUES
('Karoui', 'Alaeddine', 'alaeddine.karoui@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'ADMIN'),
('Ben Slima', 'Wissem', 'wissem.benslima@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'CONSEILLER'),
('amir', 'med', 'amir.med@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'CONSEILLER');

-- ============================================================
-- SPRINT 7: TEST DATA (Optional - Example)
-- ============================================================

INSERT INTO dossiers_credit (client_nom, client_prenom, conseiller_id, statut) VALUES
('Dupont', 'Jean', 2, 'EN_COURS');

INSERT INTO verdicts_credit (
    dossier_id,
    nombre_fiches_analysees,
    periode,
    score_risque,
    verdict,
    confiance,
    points_forts,
    risques_majeurs,
    tendances_observees,
    montant_max_recommande,
    duree_max_recommandee,
    conditions_speciales,
    taux_interet_recommande,
    justification,
    resume_court,
    utilisateur_id,
    statut
) VALUES (
    1,
    3,
    'Janvier 2024-Mars 2024',
    25,
    'VALIDE',
    0.92,
    '["Salaire stable", "Revenus garantis élevés", "Peu d''endettement"]',
    '["Revenus variables importants", "Durée contrat courte"]',
    '["Salaire croissant", "Stabilité contractuelle"]',
    50000.00,
    '24 mois',
    '["Assurance chômage recommandée", "Garantie hypothécaire"]',
    'Normal',
    'Candidat avec revenus stables et endettement faible. Profil favorable pour crédit standard avec conditions normales.',
    '✅ VALIDE - Profil stable et solvable',
    2,
    'VALIDE'
);

INSERT INTO fiches_analysees (
    verdict_id, mois, salaire_brut, salaire_net, charges, impots, bonus, primes,
    stabilite_salaire, revenus_fiables, revenus_complementaires
) VALUES
(1, 'Janvier 2024', 3000.00, 2400.00, 400.00, 200.00, 500.00, 0.00, 'stable', 3000.00, 500.00),
(1, 'Février 2024', 3000.00, 2450.00, 400.00, 150.00, 500.00, 0.00, 'stable', 3000.00, 500.00),
(1, 'Mars 2024', 3000.00, 2500.00, 400.00, 100.00, 500.00, 0.00, 'stable', 3000.00, 500.00);

-- ============================================================
-- COMMENTS & DOCUMENTATION (Sprint 7)
-- ============================================================

COMMENT ON TABLE verdicts_credit IS 'Sprint 7: Verdict final de crédit - Résultat analyse multi-fiches';
COMMENT ON TABLE fiches_analysees IS 'Sprint 7: Détails de chaque fiche de paie analysée (3-12 rows par verdict)';

COMMENT ON COLUMN verdicts_credit.score_risque IS 'Score risque 0-100: 0-30=Faible, 31-70=Modéré, 71-100=Élevé';
COMMENT ON COLUMN verdicts_credit.verdict IS 'Verdict: VALIDE (accepter), RISQUE (analyser plus), REJETE (refuser)';
COMMENT ON COLUMN verdicts_credit.montant_max_recommande IS 'Montant maximum crédit recommandé (euros)';
COMMENT ON COLUMN verdicts_credit.duree_max_recommandee IS 'Durée maximale crédit recommandée (ex: 24 mois)';
COMMENT ON COLUMN fiches_analysees.revenus_fiables IS 'Revenus garantis = salaire de base stable';
COMMENT ON COLUMN fiches_analysees.revenus_complementaires IS 'Revenus non-garantis = bonus + primes (variables)';

-- ============================================================
-- VIEWS
-- ============================================================

CREATE VIEW v_verdicts_complets AS
SELECT
    v.id as verdict_id,
    v.dossier_id,
    d.client_nom,
    d.client_prenom,
    v.nombre_fiches_analysees,
    v.periode,
    v.score_risque,
    v.verdict,
    v.confiance,
    v.montant_max_recommande,
    v.duree_max_recommandee,
    v.justification,
    v.timestamp,
    COUNT(f.id) as nombre_fiches_detaillees
FROM verdicts_credit v
JOIN dossiers_credit d ON v.dossier_id = d.id
LEFT JOIN fiches_analysees f ON v.id = f.verdict_id
GROUP BY v.id, d.id;

COMMENT ON VIEW v_verdicts_complets IS 'Vue: Verdicts avec détails dossier et nombre de fiches';
-- Nettoyage pour permettre un rerun propre sur une base de dev
DROP TABLE IF EXISTS historique_actions CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS dossiers_credit CASCADE;
DROP TABLE IF EXISTS utilisateurs CASCADE;

-- Utilisateurs
CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CONSEILLER',
    date_creation TIMESTAMP DEFAULT NOW()
);

-- Dossiers de credit
CREATE TABLE dossiers_credit (
    id BIGSERIAL PRIMARY KEY,
    client_nom VARCHAR(150) NOT NULL,
    client_prenom VARCHAR(150) NOT NULL,
    conseiller_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(30) DEFAULT 'EN_COURS',
    date_creation TIMESTAMP DEFAULT NOW()
);

-- Documents
-- Permissions (RBAC)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    ressource VARCHAR(100) NOT NULL
);
-- Historique des actions sur un dossier (traçabilité)
CREATE TABLE historique_actions (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    action VARCHAR(100) NOT NULL,
    details VARCHAR(500),
    date_action TIMESTAMP DEFAULT NOW()
);

-- Table des documents
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
-- Table des données extraites via OCR/PDF
CREATE TABLE donnees_extraites (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    dossier_id BIGINT NOT NULL REFERENCES dossiers_credit(id) ON DELETE CASCADE,
    text_complet TEXT NOT NULL,
    methode VARCHAR(50),  -- pdf_text, ocr_paddle, etc
    confidence_moyenne DECIMAL(3,2),  -- 0.00 à 1.00
    nombre_pages INTEGER,
    nombre_elements INTEGER,
    date_extraction TIMESTAMP DEFAULT NOW(),
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    statut VARCHAR(50) DEFAULT 'VALIDE'  -- VALIDE, EN_REVISION, REJETE
);

CREATE INDEX idx_donnees_document ON donnees_extraites(document_id);
CREATE INDEX idx_donnees_dossier ON donnees_extraites(dossier_id);
CREATE INDEX idx_donnees_confidence ON donnees_extraites(confidence_moyenne);

CREATE INDEX idx_documents_dossier ON documents(dossier_id);
CREATE INDEX idx_documents_statut ON documents(statut);

CREATE INDEX idx_historique_dossier ON historique_actions(dossier_id);
CREATE INDEX idx_dossiers_conseiller ON dossiers_credit(conseiller_id);




INSERT INTO permissions (role, action, ressource) VALUES
('ADMIN', 'MANAGE', 'UTILISATEURS'),
('CONSEILLER', 'CREATE', 'DOSSIER'),
('CONSEILLER', 'READ', 'DOSSIER');

-- Utilisateur de test (mot de passe encode en BCrypt pour "password123")
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role) VALUES
('Karoui', 'Alaeddine', 'alaeddine.karoui@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'ADMIN'),
('Ben Slima', 'Wissem', 'wissem.benslima@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'CONSEILLER'),
('amir', 'med', 'amir.med@bte.tn', '$2a$10$UsqLw0mSGUoG9e7fcD33b.38kTl2XCvGuYmdRlMoQBk4/.zi5T1Q6', 'CONSEILLER');


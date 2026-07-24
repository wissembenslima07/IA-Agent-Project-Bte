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
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT REFERENCES dossiers_credit(id),
    type_document VARCHAR(50),
    chemin_fichier VARCHAR(255),
    date_upload TIMESTAMP DEFAULT NOW()
);

-- Permissions (RBAC)
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    ressource VARCHAR(100) NOT NULL
);

INSERT INTO permissions (role, action, ressource) VALUES
('ADMIN', 'MANAGE', 'UTILISATEURS'),
('CONSEILLER', 'CREATE', 'DOSSIER'),
('CONSEILLER', 'READ', 'DOSSIER');

-- Utilisateur de test (mot de passe encode en BCrypt pour "password123")
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role) VALUES
('Karoui', 'Alaeddine', 'alaeddine.karoui@bte.tn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5vjeGw3.WBQhO7O9YwUFcE9XvS.wm', 'ADMIN'),
('Ben Slima', 'Wissem', 'wissem.benslima@bte.tn', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5vjeGw3.WBQhO7O9YwUFcE9XvS.wm', 'CONSEILLER');
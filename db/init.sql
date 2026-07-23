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

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT REFERENCES dossiers_credit(id),
    type_document VARCHAR(50),
    chemin_fichier VARCHAR(255),
    date_upload TIMESTAMP DEFAULT NOW()
);
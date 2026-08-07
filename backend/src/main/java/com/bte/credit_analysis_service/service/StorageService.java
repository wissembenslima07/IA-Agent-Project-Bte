package com.bte.credit_analysis_service.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {
    /**
     * Stocke un fichier et retourne son chemin/URI unique
     */
    String store(String bucketName, String fileName, MultipartFile file);

    /**
     * Récupère un fichier
     */
    InputStream retrieve(String bucketName, String filePath);

    /**
     * Supprime un fichier
     */
    void delete(String bucketName, String filePath);

    /**
     * Vérifie si un fichier existe
     */
    boolean exists(String bucketName, String filePath);
}
package com.bte.credit_analysis_service.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    @Value("${storage.local.path:/uploads}")
    private String storagePath;

    @Override
    public String store(String bucketName, String fileName, MultipartFile file) {
        try {
            // Creer la structure de dossiers : /uploads/{bucketName}/
            Path bucketPath = Paths.get(storagePath, bucketName);
            Files.createDirectories(bucketPath);

            // Generer un nom unique pour eviter les collisions
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            Path filePath = bucketPath.resolve(uniqueFileName);

            // Sauvegarder le fichier
            Files.copy(file.getInputStream(), filePath);

            log.info("Fichier stocke avec succes: {}", filePath);
            return uniqueFileName; // Retourner le nom unique pour pouvoir le recuperer plus tard
        } catch (IOException ex) {
            log.error("Erreur lors du stockage du fichier: {}", fileName, ex);
            throw new RuntimeException("Impossible de stocker le fichier: " + fileName, ex);
        }
    }

    @Override
    public InputStream retrieve(String bucketName, String filePath) {
        try {
            Path path = Paths.get(storagePath, bucketName, filePath);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Fichier non trouvé: " + filePath);
            }
            return Files.newInputStream(path);
        } catch (IOException ex) {
            log.error("Erreur lors de la recuperation du fichier: {}", filePath, ex);
            throw new RuntimeException("Impossible de recuperer le fichier", ex);
        }
    }

    @Override
    public void delete(String bucketName, String filePath) {
        try {
            Path path = Paths.get(storagePath, bucketName, filePath);
            Files.deleteIfExists(path);
            log.info("Fichier supprime: {}", filePath);
        } catch (IOException ex) {
            log.error("Erreur lors de la suppression du fichier: {}", filePath, ex);
            throw new RuntimeException("Impossible de supprimer le fichier", ex);
        }
    }

    @Override
    public boolean exists(String bucketName, String filePath) {
        Path path = Paths.get(storagePath, bucketName, filePath);
        return Files.exists(path);
    }
}
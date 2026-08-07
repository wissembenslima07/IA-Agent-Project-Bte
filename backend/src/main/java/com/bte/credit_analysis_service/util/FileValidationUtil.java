package com.bte.credit_analysis_service.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class FileValidationUtil {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final long MAX_TOTAL_SIZE = 100 * 1024 * 1024; // 100 MB par requête
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx"
    );

    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        // Verifier la taille
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                "La taille du fichier dépasse la limite de 10 Mo"
            );
        }

        // Verifier le type MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Type de fichier non autorisé: " + contentType
            );
        }

        // Verifier l'extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || !hasAllowedExtension(fileName)) {
            throw new IllegalArgumentException(
                "Extension de fichier non autorisée: " + fileName
            );
        }
    }

    public static void validateTotalSize(MultipartFile[] files) {
        long totalSize = 0;
        for (MultipartFile file : files) {
            totalSize += file.getSize();
        }
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new IllegalArgumentException(
                "La taille totale des fichiers dépasse la limite de 100 Mo"
            );
        }
    }

    private static boolean hasAllowedExtension(String fileName) {
        String extension = getExtension(fileName).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}
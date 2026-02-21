package com.sara.tfgdam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class ExcelTemplateSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ExcelTemplateSnapshotService.class);

    @Value("${sara.import.storage-path:./storage/imports-ra}")
    private String importStoragePath;

    public void saveImportedTemplate(Long moduleId, MultipartFile file) {
        if (moduleId == null || file == null || file.isEmpty()) {
            return;
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!originalFilename.endsWith(".xlsx")) {
            return;
        }

        Path directory = Paths.get(importStoragePath).toAbsolutePath().normalize();
        Path target = directory.resolve(buildSnapshotFileName(moduleId));

        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException ex) {
            // Snapshot is a best-effort feature and must not break excel import.
            log.warn("Cannot store imported excel snapshot for module {}: {}", moduleId, ex.getMessage());
        }
    }

    public Path resolveSnapshotPath(Long moduleId) {
        if (moduleId == null) {
            return null;
        }

        Path directory = Paths.get(importStoragePath).toAbsolutePath().normalize();
        Path preferred = directory.resolve(buildSnapshotFileName(moduleId));
        if (Files.exists(preferred)) {
            return preferred;
        }

        // Backward compatibility with legacy naming in storage/imports-ra.
        Path legacy = directory.resolve(moduleId + "_source_template_rellenada.xlsx");
        if (Files.exists(legacy)) {
            return legacy;
        }

        return null;
    }

    private String buildSnapshotFileName(Long moduleId) {
        return "module-" + moduleId + "-source-template.xlsx";
    }
}

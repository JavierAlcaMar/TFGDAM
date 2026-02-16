package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.ImportJob;
import com.sara.tfgdam.domain.entity.ImportJobStatus;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final ExtractorClient extractorClient;

    @Value("${sara.import.storage-path:./storage/imports-ra}")
    private String importStoragePath;

    public ImportJob createRAImportJob(Long moduleId, MultipartFile file) {
        validateInput(moduleId, file);

        String originalFilename = file.getOriginalFilename() == null
                ? "ra-import.bin"
                : file.getOriginalFilename().trim();

        ImportJob job = ImportJob.builder()
                .moduleId(moduleId)
                .filename(originalFilename)
                .status(ImportJobStatus.UPLOADED)
                .build();
        job = importJobRepository.save(job);

        try {
            Path savedPath = storeFile(job.getId(), originalFilename, file);

            job.setStatus(ImportJobStatus.PROCESSING);
            job.setErrorMessage(null);
            job = importJobRepository.save(job);

            String extractedJson = extractorClient.extractRAs(savedPath, originalFilename);
            if (extractedJson == null || extractedJson.trim().isEmpty()) {
                throw new BusinessValidationException("Extractor returned empty JSON result");
            }

            job.setResultJson(extractedJson);
            job.setStatus(ImportJobStatus.PARSED);
            job.setErrorMessage(null);
            return importJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            return importJobRepository.save(job);
        }
    }

    public ImportJob getJob(Long jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
    }

    private void validateInput(Long moduleId, MultipartFile file) {
        if (moduleId == null) {
            throw new BusinessValidationException("moduleId is required");
        }

        courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));

        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("file is required and cannot be empty");
        }
    }

    private Path storeFile(Long jobId, String originalFilename, MultipartFile file) {
        String normalizedName = originalFilename
                .replace("..", "")
                .replace("/", "_")
                .replace("\\", "_");

        Path directory = Paths.get(importStoragePath).toAbsolutePath().normalize();
        Path target = directory.resolve(jobId + "_" + normalizedName);

        try {
            Files.createDirectories(directory);
            file.transferTo(target);
            return target;
        } catch (IOException ex) {
            throw new BusinessValidationException("Cannot store uploaded file: " + ex.getMessage());
        }
    }
}

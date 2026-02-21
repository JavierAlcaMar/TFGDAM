package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.ExcelImportRequest;
import com.sara.tfgdam.dto.ExcelImportResponse;
import com.sara.tfgdam.dto.ImportJobResponse;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.ExcelJsonImportService;
import com.sara.tfgdam.service.ExcelTemplateSnapshotService;
import com.sara.tfgdam.service.ExcelTemplateMapperService;
import com.sara.tfgdam.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/imports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class ImportController {

    private final ImportService importService;
    private final ExcelJsonImportService excelJsonImportService;
    private final ExcelTemplateMapperService excelTemplateMapperService;
    private final ExcelTemplateSnapshotService excelTemplateSnapshotService;
    private final DtoMapper mapper;

    @PostMapping(value = "/ra", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportJobResponse createRAImportJob(@RequestParam Long moduleId,
                                               @RequestPart("file") MultipartFile file) {
        return mapper.toImportJobResponse(importService.createRAImportJob(moduleId, file));
    }

    @PostMapping(value = "/excel-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ExcelImportResponse importExcelJson(@Valid @RequestBody ExcelImportRequest request,
                                               @RequestParam(required = false) String moduleId) {
        return excelJsonImportService.importExcelJson(request, parseOptionalModuleId(moduleId));
    }

    @PostMapping(value = "/excel-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ExcelImportResponse importExcelFile(@RequestParam(required = false) String moduleId,
                                               @RequestPart("file") MultipartFile file) {
        ExcelImportRequest request = excelTemplateMapperService.mapTemplate(file);
        ExcelImportResponse response = excelJsonImportService.importExcelJson(request, parseOptionalModuleId(moduleId));
        excelTemplateSnapshotService.saveImportedTemplate(response.getModuleId(), file);
        return response;
    }

    @GetMapping("/ra/{jobId}")
    public ImportJobResponse getRAImportJob(@PathVariable Long jobId) {
        return mapper.toImportJobResponse(importService.getJob(jobId));
    }

    private Long parseOptionalModuleId(String rawModuleId) {
        if (rawModuleId == null || rawModuleId.trim().isEmpty()) {
            return null;
        }

        String normalized = rawModuleId.trim();
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0) {
                throw new BusinessValidationException("moduleId must be a positive number");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessValidationException("moduleId must be a number");
        }
    }
}

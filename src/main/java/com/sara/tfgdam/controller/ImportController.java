package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.ImportJobResponse;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final DtoMapper mapper;

    @PostMapping(value = "/ra", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportJobResponse createRAImportJob(@RequestParam Long moduleId,
                                               @RequestPart("file") MultipartFile file) {
        return mapper.toImportJobResponse(importService.createRAImportJob(moduleId, file));
    }

    @GetMapping("/ra/{jobId}")
    public ImportJobResponse getRAImportJob(@PathVariable Long jobId) {
        return mapper.toImportJobResponse(importService.getJob(jobId));
    }
}

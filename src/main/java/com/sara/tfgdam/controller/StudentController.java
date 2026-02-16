package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.CreateStudentRequest;
import com.sara.tfgdam.dto.StudentReportResponse;
import com.sara.tfgdam.dto.StudentResponse;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.CalculationService;
import com.sara.tfgdam.service.ModuleSetupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class StudentController {

    private final ModuleSetupService moduleSetupService;
    private final CalculationService calculationService;
    private final DtoMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return mapper.toStudentResponse(moduleSetupService.createStudent(request));
    }

    @GetMapping("/{id}/report")
    public StudentReportResponse getStudentReport(@PathVariable Long id,
                                                  @RequestParam @NotNull Long moduleId) {
        return calculationService.getStudentReport(id, moduleId);
    }
}

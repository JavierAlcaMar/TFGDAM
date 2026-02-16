package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.GradeBatchRequest;
import com.sara.tfgdam.dto.GradeResponse;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/grades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class GradeController {

    private final GradeService gradeService;
    private final DtoMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<GradeResponse> upsertGrades(@Valid @RequestBody GradeBatchRequest request) {
        return gradeService.upsertGrades(request).stream()
                .map(mapper::toGradeResponse)
                .toList();
    }
}

package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.CreateTeacherRequest;
import com.sara.tfgdam.dto.TeacherResponse;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.ModuleSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class TeacherController {

    private final ModuleSetupService moduleSetupService;
    private final DtoMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        return mapper.toTeacherResponse(moduleSetupService.createTeacher(request));
    }
}

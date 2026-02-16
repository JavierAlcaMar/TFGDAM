package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.PatchRARequest;
import com.sara.tfgdam.dto.RAResponse;
import com.sara.tfgdam.dto.UpdateRARequest;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.ModuleSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ras")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class RAController {

    private final ModuleSetupService moduleSetupService;
    private final DtoMapper mapper;

    @PutMapping("/{raId}")
    public RAResponse updateRA(@PathVariable Long raId, @Valid @RequestBody UpdateRARequest request) {
        return mapper.toRAResponse(moduleSetupService.updateRA(raId, request));
    }

    @PatchMapping("/{raId}")
    public RAResponse patchRA(@PathVariable Long raId, @Valid @RequestBody PatchRARequest request) {
        return mapper.toRAResponse(moduleSetupService.patchRA(raId, request));
    }

    @DeleteMapping("/{raId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRA(@PathVariable Long raId) {
        moduleSetupService.deleteRA(raId);
    }
}

package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.CreateInstrumentRequest;
import com.sara.tfgdam.dto.InstrumentResponse;
import com.sara.tfgdam.dto.PatchUTRequest;
import com.sara.tfgdam.dto.UTResponse;
import com.sara.tfgdam.dto.UpdateUTRequest;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.ModuleSetupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class UTController {

    private final ModuleSetupService moduleSetupService;
    private final DtoMapper mapper;

    @PostMapping("/{utId}/instruments")
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse createInstrument(@PathVariable Long utId,
                                               @Valid @RequestBody CreateInstrumentRequest request) {
        return mapper.toInstrumentResponse(moduleSetupService.createInstrument(utId, request));
    }

    @PutMapping("/{utId}")
    public UTResponse updateUT(@PathVariable Long utId, @Valid @RequestBody UpdateUTRequest request) {
        var ut = moduleSetupService.updateUT(utId, request);
        return mapper.toUTResponse(ut, moduleSetupService.getActivityIdByUT(ut.getId()));
    }

    @PatchMapping("/{utId}")
    public UTResponse patchUT(@PathVariable Long utId, @Valid @RequestBody PatchUTRequest request) {
        var ut = moduleSetupService.patchUT(utId, request);
        return mapper.toUTResponse(ut, moduleSetupService.getActivityIdByUT(ut.getId()));
    }

    @DeleteMapping("/{utId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUT(@PathVariable Long utId) {
        moduleSetupService.deleteUT(utId);
    }
}

package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.InstrumentRAResponse;
import com.sara.tfgdam.dto.InstrumentResponse;
import com.sara.tfgdam.dto.PatchInstrumentRARequest;
import com.sara.tfgdam.dto.PatchInstrumentRequest;
import com.sara.tfgdam.dto.SetInstrumentRAsRequest;
import com.sara.tfgdam.dto.UpdateInstrumentRequest;
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
@RequestMapping("/instruments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class InstrumentController {

    private final ModuleSetupService moduleSetupService;
    private final DtoMapper mapper;

    @PostMapping("/{id}/ras")
    public InstrumentRAResponse createOrReplaceInstrumentRAs(@PathVariable Long id,
                                                             @Valid @RequestBody SetInstrumentRAsRequest request) {
        return moduleSetupService.setInstrumentRAs(id, request);
    }

    @PutMapping("/{id}/ras")
    public InstrumentRAResponse putInstrumentRAs(@PathVariable Long id,
                                                 @Valid @RequestBody SetInstrumentRAsRequest request) {
        return moduleSetupService.setInstrumentRAs(id, request);
    }

    @PatchMapping("/{id}/ras")
    public InstrumentRAResponse patchInstrumentRAs(@PathVariable Long id,
                                                   @Valid @RequestBody PatchInstrumentRARequest request) {
        return moduleSetupService.patchInstrumentRAs(id, request);
    }

    @DeleteMapping("/{id}/ras/{raId}")
    public InstrumentRAResponse deleteInstrumentRA(@PathVariable Long id, @PathVariable Long raId) {
        return moduleSetupService.deleteInstrumentRA(id, raId);
    }

    @DeleteMapping("/{id}/ras")
    public InstrumentRAResponse clearInstrumentRAs(@PathVariable Long id) {
        return moduleSetupService.clearInstrumentRAs(id);
    }

    @PutMapping("/{id}")
    public InstrumentResponse updateInstrument(@PathVariable Long id,
                                               @Valid @RequestBody UpdateInstrumentRequest request) {
        return mapper.toInstrumentResponse(moduleSetupService.updateInstrument(id, request));
    }

    @PatchMapping("/{id}")
    public InstrumentResponse patchInstrument(@PathVariable Long id,
                                              @Valid @RequestBody PatchInstrumentRequest request) {
        return mapper.toInstrumentResponse(moduleSetupService.patchInstrument(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInstrument(@PathVariable Long id) {
        moduleSetupService.deleteInstrument(id);
    }
}

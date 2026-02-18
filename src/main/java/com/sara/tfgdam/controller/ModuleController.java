package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.CreateModuleRequest;
import com.sara.tfgdam.dto.CreateRARequest;
import com.sara.tfgdam.dto.CreateUTRequest;
import com.sara.tfgdam.dto.ImportRAsConfirmRequest;
import com.sara.tfgdam.dto.ModuleEvaluationReportResponse;
import com.sara.tfgdam.dto.ModuleFinalReportResponse;
import com.sara.tfgdam.dto.ModulePreviewResponse;
import com.sara.tfgdam.dto.ModuleResponse;
import com.sara.tfgdam.dto.PatchUTRALinkRequest;
import com.sara.tfgdam.dto.RAResponse;
import com.sara.tfgdam.dto.UTRALinkResponse;
import com.sara.tfgdam.dto.UTResponse;
import com.sara.tfgdam.dto.UpdateUTRALinkRequest;
import com.sara.tfgdam.dto.UpsertUTRALinkRequest;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.CalculationService;
import com.sara.tfgdam.service.ModuleSetupService;
import com.sara.tfgdam.service.ModulePreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','DIRECTOR','SUPERADMIN')")
public class ModuleController {

    private final ModuleSetupService moduleSetupService;
    private final ModulePreviewService modulePreviewService;
    private final CalculationService calculationService;
    private final DtoMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleResponse createModule(@Valid @RequestBody CreateModuleRequest request) {
        return mapper.toModuleResponse(moduleSetupService.createModule(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable Long id) {
        moduleSetupService.deleteModule(id);
    }

    @GetMapping("/{id}/ras")
    public List<RAResponse> getRAsByModule(@PathVariable Long id) {
        return moduleSetupService.getRAsByModuleId(id).stream()
                .map(mapper::toRAResponse)
                .toList();
    }

    @PostMapping("/{id}/ras")
    @ResponseStatus(HttpStatus.CREATED)
    public RAResponse createRA(@PathVariable Long id, @Valid @RequestBody CreateRARequest request) {
        return mapper.toRAResponse(moduleSetupService.createRA(id, request));
    }

    @PostMapping("/{moduleId}/ras/import")
    public List<RAResponse> importRAs(@PathVariable Long moduleId,
                                      @Valid @RequestBody ImportRAsConfirmRequest request) {
        return moduleSetupService.importRAs(moduleId, request).stream()
                .map(mapper::toRAResponse)
                .toList();
    }

    @PostMapping("/{id}/uts")
    @ResponseStatus(HttpStatus.CREATED)
    public UTResponse createUT(@PathVariable Long id, @Valid @RequestBody CreateUTRequest request) {
        var ut = moduleSetupService.createUT(id, request);
        return mapper.toUTResponse(ut, moduleSetupService.getActivityIdByUT(ut.getId()));
    }

    @PostMapping("/{id}/ut-ra")
    @ResponseStatus(HttpStatus.CREATED)
    public UTRALinkResponse upsertUTRA(@PathVariable Long id, @Valid @RequestBody UpsertUTRALinkRequest request) {
        return mapper.toUTRALinkResponse(moduleSetupService.upsertUTRALink(id, request));
    }

    @PutMapping("/{id}/ut-ra/{linkId}")
    public UTRALinkResponse updateUTRALink(@PathVariable Long id,
                                           @PathVariable Long linkId,
                                           @Valid @RequestBody UpdateUTRALinkRequest request) {
        return mapper.toUTRALinkResponse(moduleSetupService.updateUTRALink(id, linkId, request));
    }

    @PatchMapping("/{id}/ut-ra/{linkId}")
    public UTRALinkResponse patchUTRALink(@PathVariable Long id,
                                          @PathVariable Long linkId,
                                          @Valid @RequestBody PatchUTRALinkRequest request) {
        return mapper.toUTRALinkResponse(moduleSetupService.patchUTRALink(id, linkId, request));
    }

    @DeleteMapping("/{id}/ut-ra/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUTRALink(@PathVariable Long id, @PathVariable Long linkId) {
        moduleSetupService.deleteUTRALink(id, linkId);
    }

    @GetMapping("/{id}/reports/evaluation/{n}")
    public ModuleEvaluationReportResponse moduleEvaluationReport(@PathVariable Long id, @PathVariable Integer n) {
        return calculationService.getModuleEvaluationReport(id, n);
    }

    @GetMapping("/{id}/reports/final")
    public ModuleFinalReportResponse moduleFinalReport(@PathVariable Long id) {
        return calculationService.getModuleFinalReport(id);
    }

    @GetMapping("/{id}/preview")
    public ModulePreviewResponse modulePreview(@PathVariable Long id) {
        return modulePreviewService.getPreview(id);
    }
}

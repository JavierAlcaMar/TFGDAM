package com.sara.tfgdam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ImportRAsConfirmRequest {

    @Valid
    @NotEmpty(message = "ras cannot be empty")
    private List<ImportRAItemDto> ras;
}

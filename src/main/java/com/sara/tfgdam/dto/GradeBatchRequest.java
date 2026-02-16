package com.sara.tfgdam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradeBatchRequest {

    @Valid
    @NotEmpty(message = "grades cannot be empty")
    private List<GradeEntryRequest> grades;
}

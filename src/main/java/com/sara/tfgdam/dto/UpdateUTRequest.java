package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUTRequest {

    @NotBlank(message = "UT name is required")
    private String name;

    @NotNull(message = "evaluationPeriod is required")
    @Positive(message = "evaluationPeriod must be > 0")
    private Integer evaluationPeriod;
}

package com.sara.tfgdam.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchUTRequest {

    private String name;

    @Positive(message = "evaluationPeriod must be > 0")
    private Integer evaluationPeriod;
}

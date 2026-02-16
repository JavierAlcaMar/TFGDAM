package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GradeEntryRequest {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotNull(message = "instrumentId is required")
    private Long instrumentId;

    @NotNull(message = "gradeValue is required")
    @DecimalMin(value = "0.00", message = "gradeValue must be >= 0")
    @DecimalMax(value = "10.00", message = "gradeValue must be <= 10")
    private BigDecimal gradeValue;
}

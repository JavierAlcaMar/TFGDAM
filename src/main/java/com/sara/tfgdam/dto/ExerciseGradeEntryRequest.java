package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExerciseGradeEntryRequest {

    @NotNull(message = "exerciseIndex is required")
    @Min(value = 1, message = "exerciseIndex must be >= 1")
    @Max(value = 10, message = "exerciseIndex must be <= 10")
    private Integer exerciseIndex;

    @NotNull(message = "gradeValue is required")
    @DecimalMin(value = "0.00", message = "gradeValue must be >= 0")
    @DecimalMax(value = "10.00", message = "gradeValue must be <= 10")
    private BigDecimal gradeValue;
}


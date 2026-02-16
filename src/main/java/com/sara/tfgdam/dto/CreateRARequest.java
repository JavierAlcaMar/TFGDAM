package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateRARequest {

    @NotBlank(message = "RA code is required")
    private String code;

    @NotBlank(message = "RA name is required")
    private String name;

    @NotNull(message = "RA weightPercent is required")
    @DecimalMin(value = "0.00", message = "RA weightPercent must be >= 0")
    @DecimalMax(value = "100.00", message = "RA weightPercent must be <= 100")
    private BigDecimal weightPercent;
}

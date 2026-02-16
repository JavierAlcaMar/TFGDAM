package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PatchRARequest {

    private String code;

    private String name;

    @DecimalMin(value = "0.00", message = "RA weightPercent must be >= 0")
    @DecimalMax(value = "100.00", message = "RA weightPercent must be <= 100")
    private BigDecimal weightPercent;
}

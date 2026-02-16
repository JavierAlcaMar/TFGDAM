package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpsertUTRALinkRequest {

    @NotNull(message = "utId is required")
    private Long utId;

    @NotNull(message = "raId is required")
    private Long raId;

    @NotNull(message = "percent is required")
    @DecimalMin(value = "0.00", message = "percent must be >= 0")
    @DecimalMax(value = "100.00", message = "percent must be <= 100")
    private BigDecimal percent;
}

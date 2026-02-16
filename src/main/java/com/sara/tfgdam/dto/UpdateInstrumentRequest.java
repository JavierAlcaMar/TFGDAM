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
public class UpdateInstrumentRequest {

    @NotBlank(message = "Instrument name is required")
    private String name;

    @NotNull(message = "Instrument weightPercent is required")
    @DecimalMin(value = "0.00", message = "Instrument weightPercent must be >= 0")
    @DecimalMax(value = "100.00", message = "Instrument weightPercent must be <= 100")
    private BigDecimal weightPercent;
}

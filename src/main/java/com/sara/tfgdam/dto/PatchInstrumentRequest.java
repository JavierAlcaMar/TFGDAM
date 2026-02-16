package com.sara.tfgdam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PatchInstrumentRequest {

    private String name;

    @DecimalMin(value = "0.00", message = "Instrument weightPercent must be >= 0")
    @DecimalMax(value = "100.00", message = "Instrument weightPercent must be <= 100")
    private BigDecimal weightPercent;
}

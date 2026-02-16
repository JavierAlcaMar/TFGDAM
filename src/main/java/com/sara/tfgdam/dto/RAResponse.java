package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RAResponse {
    Long id;
    Long moduleId;
    String code;
    String name;
    BigDecimal weightPercent;
}

package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InstrumentResponse {
    Long id;
    Long activityId;
    Long utId;
    String name;
    BigDecimal weightPercent;
}

package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class UTRALinkResponse {
    Long id;
    Long utId;
    Long raId;
    BigDecimal percent;
}

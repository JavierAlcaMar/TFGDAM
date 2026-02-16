package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RAGradeDto {
    Long raId;
    String raCode;
    String raName;
    BigDecimal grade;
}

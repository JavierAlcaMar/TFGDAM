package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GradeResponse {
    Long id;
    Long studentId;
    Long instrumentId;
    BigDecimal gradeValue;
}

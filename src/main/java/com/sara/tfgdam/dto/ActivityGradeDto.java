package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ActivityGradeDto {
    Long activityId;
    Long utId;
    String activityName;
    Integer evaluationPeriod;
    BigDecimal grade;
}

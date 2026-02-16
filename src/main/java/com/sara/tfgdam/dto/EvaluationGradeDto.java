package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class EvaluationGradeDto {
    Integer evaluationPeriod;
    BigDecimal numericGrade;
    Integer suggestedBulletinGrade;
    boolean allRAsPassed;
}

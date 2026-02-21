package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class GradeResponse {
    Long id;
    Long studentId;
    Long instrumentId;
    BigDecimal gradeValue;
    List<ExerciseGradeItem> exerciseGrades;

    @Value
    @Builder
    public static class ExerciseGradeItem {
        Integer exerciseIndex;
        BigDecimal gradeValue;
    }
}

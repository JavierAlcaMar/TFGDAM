package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class StudentReportResponse {
    Long studentId;
    Long moduleId;
    List<ActivityGradeDto> activityGrades;
    List<RAGradeDto> raGrades;
    List<EvaluationGradeDto> evaluationGrades;
    BigDecimal finalGrade;
}

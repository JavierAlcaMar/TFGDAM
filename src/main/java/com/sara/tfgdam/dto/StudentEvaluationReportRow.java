package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class StudentEvaluationReportRow {
    Long studentId;
    String studentCode;
    String studentName;
    BigDecimal numericGrade;
    Integer suggestedBulletinGrade;
    boolean allRAsPassed;
}

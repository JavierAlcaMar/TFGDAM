package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class StudentFinalReportRow {
    Long studentId;
    String studentCode;
    String studentName;
    BigDecimal finalGrade;
}

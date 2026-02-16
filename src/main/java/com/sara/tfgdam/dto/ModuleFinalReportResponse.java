package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ModuleFinalReportResponse {
    Long moduleId;
    List<StudentFinalReportRow> students;
}

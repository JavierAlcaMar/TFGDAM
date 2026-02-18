package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ExcelImportResponse {
    Long moduleId;
    Integer raCount;
    Integer utCount;
    Integer instrumentCount;
    Integer studentCount;
    Integer gradeCount;
    Map<String, Long> raIdsByCode;
    Map<String, Long> utIdsByKey;
    Map<String, Long> instrumentIdsByKey;
    Map<String, Long> studentIdsByCode;
}

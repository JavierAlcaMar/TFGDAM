package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModuleResponse {
    Long id;
    String name;
    String academicYear;
    Long teacherId;
    String teacherName;
}

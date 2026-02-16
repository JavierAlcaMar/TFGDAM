package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateModuleRequest {

    @NotBlank(message = "Module name is required")
    private String name;

    private String academicYear;

    private Long teacherId;

    private String teacherName;
}

package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStudentRequest {

    @NotNull(message = "moduleId is required")
    private Long moduleId;

    @NotBlank(message = "studentCode is required")
    private String studentCode;

    @NotBlank(message = "fullName is required")
    private String fullName;
}

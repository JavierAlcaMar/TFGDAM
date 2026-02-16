package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeacherRequest {

    @NotBlank(message = "Teacher fullName is required")
    private String fullName;
}

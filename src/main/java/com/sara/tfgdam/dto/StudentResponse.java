package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentResponse {
    Long id;
    Long moduleId;
    String studentCode;
    String fullName;
}

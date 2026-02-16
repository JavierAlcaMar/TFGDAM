package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherResponse {
    Long id;
    String fullName;
}

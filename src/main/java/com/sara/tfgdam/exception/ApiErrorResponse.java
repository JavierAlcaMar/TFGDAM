package com.sara.tfgdam.exception;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Map;

@Value
@Builder
public class ApiErrorResponse {
    OffsetDateTime timestamp;
    int status;
    String error;
    String code;
    String message;
    String path;
    Map<String, String> details;
}

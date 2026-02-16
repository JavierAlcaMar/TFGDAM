package com.sara.tfgdam.dto;

import com.sara.tfgdam.domain.entity.ImportJobStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class ImportJobResponse {
    Long id;
    Long moduleId;
    String filename;
    ImportJobStatus status;
    String resultJson;
    String errorMessage;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}

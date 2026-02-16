package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UTResponse {
    Long id;
    Long moduleId;
    Long activityId;
    String name;
    Integer evaluationPeriod;
}

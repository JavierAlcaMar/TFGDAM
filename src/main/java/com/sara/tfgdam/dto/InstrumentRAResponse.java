package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InstrumentRAResponse {
    Long instrumentId;
    Long utId;
    List<Long> raIds;
}

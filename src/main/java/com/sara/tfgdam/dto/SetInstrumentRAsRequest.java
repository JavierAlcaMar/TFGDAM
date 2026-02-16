package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SetInstrumentRAsRequest {

    @NotEmpty(message = "raIds cannot be empty")
    private List<@NotNull(message = "raId cannot be null") Long> raIds;
}

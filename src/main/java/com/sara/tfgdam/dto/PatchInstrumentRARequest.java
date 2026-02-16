package com.sara.tfgdam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatchInstrumentRARequest {

    private List<@NotNull(message = "addRaIds cannot contain null values") Long> addRaIds;

    private List<@NotNull(message = "removeRaIds cannot contain null values") Long> removeRaIds;
}

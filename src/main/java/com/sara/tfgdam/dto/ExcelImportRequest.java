package com.sara.tfgdam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ExcelImportRequest {

    @Valid
    @NotNull(message = "module is required")
    private CreateModuleRequest module;

    @Valid
    @NotEmpty(message = "ras cannot be empty")
    private List<ImportRAItemDto> ras;

    @Valid
    @NotEmpty(message = "uts cannot be empty")
    private List<UTItem> uts;

    @Valid
    private List<StudentItem> students;

    @Getter
    @Setter
    public static class UTItem {

        @NotBlank(message = "UT key is required")
        private String key;

        @NotBlank(message = "UT name is required")
        private String name;

        @NotNull(message = "evaluationPeriod is required")
        @Positive(message = "evaluationPeriod must be > 0")
        private Integer evaluationPeriod;

        @Valid
        @NotEmpty(message = "raDistributions cannot be empty")
        private List<UTRADistributionItem> raDistributions;

        @Valid
        @NotEmpty(message = "instruments cannot be empty")
        private List<InstrumentItem> instruments;
    }

    @Getter
    @Setter
    public static class UTRADistributionItem {

        @NotBlank(message = "raCode is required")
        private String raCode;

        @NotNull(message = "percent is required")
        @DecimalMin(value = "0.00", message = "percent must be >= 0")
        @DecimalMax(value = "100.00", message = "percent must be <= 100")
        private BigDecimal percent;
    }

    @Getter
    @Setter
    public static class InstrumentItem {

        @NotBlank(message = "Instrument key is required")
        private String key;

        @NotBlank(message = "Instrument name is required")
        private String name;

        @NotNull(message = "Instrument weightPercent is required")
        @DecimalMin(value = "0.00", message = "Instrument weightPercent must be >= 0")
        @DecimalMax(value = "100.00", message = "Instrument weightPercent must be <= 100")
        private BigDecimal weightPercent;

        @NotEmpty(message = "raCodes cannot be empty")
        private List<@NotBlank(message = "raCode cannot be blank") String> raCodes;
    }

    @Getter
    @Setter
    public static class StudentItem {

        @NotBlank(message = "studentCode is required")
        private String studentCode;

        @NotBlank(message = "fullName is required")
        private String fullName;

        @Valid
        private List<GradeItem> grades;
    }

    @Getter
    @Setter
    public static class GradeItem {

        @NotBlank(message = "instrumentKey is required")
        private String instrumentKey;

        @NotNull(message = "gradeValue is required")
        @DecimalMin(value = "0.00", message = "gradeValue must be >= 0")
        @DecimalMax(value = "10.00", message = "gradeValue must be <= 10")
        private BigDecimal gradeValue;
    }
}

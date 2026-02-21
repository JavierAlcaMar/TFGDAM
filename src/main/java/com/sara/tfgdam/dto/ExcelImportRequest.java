package com.sara.tfgdam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Valid
    private List<EvaluationOverrideItem> evaluationOverrides;

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

        @Valid
        private List<ExerciseWeightItem> exerciseWeights;
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

        @Valid
        private List<ExerciseGradeItem> exerciseGrades;
    }

    @Getter
    @Setter
    public static class ExerciseWeightItem {

        @NotNull(message = "exerciseIndex is required")
        @Min(value = 1, message = "exerciseIndex must be >= 1")
        @Max(value = 10, message = "exerciseIndex must be <= 10")
        private Integer exerciseIndex;

        @NotNull(message = "weightPercent is required")
        @DecimalMin(value = "0.00", message = "weightPercent must be >= 0")
        @DecimalMax(value = "100.00", message = "weightPercent must be <= 100")
        private BigDecimal weightPercent;
    }

    @Getter
    @Setter
    public static class ExerciseGradeItem {

        @NotNull(message = "exerciseIndex is required")
        @Min(value = 1, message = "exerciseIndex must be >= 1")
        @Max(value = 10, message = "exerciseIndex must be <= 10")
        private Integer exerciseIndex;

        @NotNull(message = "gradeValue is required")
        @DecimalMin(value = "0.00", message = "gradeValue must be >= 0")
        @DecimalMax(value = "10.00", message = "gradeValue must be <= 10")
        private BigDecimal gradeValue;
    }

    @Getter
    @Setter
    public static class EvaluationOverrideItem {

        @NotBlank(message = "studentCode is required for evaluationOverride")
        private String studentCode;

        @NotNull(message = "evaluationPeriod is required for evaluationOverride")
        @Positive(message = "evaluationPeriod must be > 0")
        private Integer evaluationPeriod;

        @NotNull(message = "numericGrade is required for evaluationOverride")
        @DecimalMin(value = "0.00", message = "numericGrade must be >= 0")
        @DecimalMax(value = "10.00", message = "numericGrade must be <= 10")
        private BigDecimal numericGrade;

        @NotNull(message = "suggestedBulletinGrade is required for evaluationOverride")
        private Integer suggestedBulletinGrade;

        @NotNull(message = "allRAsPassed is required for evaluationOverride")
        private Boolean allRAsPassed;

        @PositiveOrZero(message = "failedRasCount must be >= 0")
        private Integer failedRasCount;
    }
}

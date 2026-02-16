package com.sara.tfgdam.service;

import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.repository.UTRALinkRepository;
import com.sara.tfgdam.validation.ConfigurationValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationServiceTest {

    private final CalculationService calculationService = new CalculationService(
            (CourseModuleRepository) null,
            (LearningOutcomeRARepository) null,
            (TeachingUnitUTRepository) null,
            (UTRALinkRepository) null,
            (ActivityRepository) null,
            (InstrumentRepository) null,
            (InstrumentRARepository) null,
            (StudentRepository) null,
            (GradeRepository) null,
            (ConfigurationValidator) null
    );

    @Test
    void bulletinGrade_whenNumericLowerThanOne_returnsOne() {
        int result = calculationService.calculateSuggestedBulletinGrade(new BigDecimal("0.75"), false);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void bulletinGrade_whenNumericLowerThanFive_truncates() {
        int result = calculationService.calculateSuggestedBulletinGrade(new BigDecimal("4.95"), false);

        assertThat(result).isEqualTo(4);
    }

    @Test
    void bulletinGrade_whenNumericAtLeastFiveAndAnyRaFailed_returnsFour() {
        int result = calculationService.calculateSuggestedBulletinGrade(new BigDecimal("7.10"), false);

        assertThat(result).isEqualTo(4);
    }

    @Test
    void bulletinGrade_whenNumericAtLeastFiveAndAllRaPassed_roundsNearestInteger() {
        int result = calculationService.calculateSuggestedBulletinGrade(new BigDecimal("6.60"), true);

        assertThat(result).isEqualTo(7);
    }
}

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock
    private CourseModuleRepository courseModuleRepository;
    @Mock
    private LearningOutcomeRARepository learningOutcomeRARepository;
    @Mock
    private TeachingUnitUTRepository teachingUnitUTRepository;
    @Mock
    private UTRALinkRepository utraLinkRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private InstrumentRARepository instrumentRARepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private ConfigurationValidator configurationValidator;

    @InjectMocks
    private CalculationService calculationService;

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

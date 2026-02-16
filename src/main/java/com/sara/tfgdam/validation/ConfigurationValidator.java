package com.sara.tfgdam.validation;

import com.sara.tfgdam.domain.entity.Activity;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.repository.UTRALinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ConfigurationValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final UTRALinkRepository utraLinkRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;

    public void validateRAWeightDoesNotExceed100(Long moduleId) {
        BigDecimal sum = learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .map(LearningOutcomeRA::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) > 0) {
            throw new BusinessValidationException("In module " + moduleId + ", sum of RA weights cannot exceed 100. Current=" + sum);
        }
    }

    public void validateRADistributionDoesNotExceed100(Long raId) {
        BigDecimal sum = utraLinkRepository.findByLearningOutcomeId(raId).stream()
                .map(link -> link.getPercent())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) > 0) {
            throw new BusinessValidationException("For RA " + raId + ", sum of UT-RA distribution cannot exceed 100. Current=" + sum);
        }
    }

    public void validateInstrumentWeightsDoNotExceed100(Long activityId) {
        BigDecimal sum = instrumentRepository.findByActivityId(activityId).stream()
                .map(Instrument::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) > 0) {
            throw new BusinessValidationException("For activity " + activityId + ", sum of instrument weights cannot exceed 100. Current=" + sum);
        }
    }

    public void validateInstrumentRAsAllowedByUT(Activity activity, Set<Long> raIds) {
        Long utId = activity.getTeachingUnit().getId();
        Set<Long> allowedRaIds = utraLinkRepository.findByTeachingUnitId(utId).stream()
                .filter(link -> link.getPercent().compareTo(ZERO) > 0)
                .map(link -> link.getLearningOutcome().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (Long raId : raIds) {
            if (!allowedRaIds.contains(raId)) {
                throw new BusinessValidationException(
                        "Instrument can only be associated to RAs with UT-RA percent > 0 in this UT. Invalid raId=" + raId
                );
            }
        }
    }

    public void validateModuleReadyForCalculations(Long moduleId) {
        validateRAWeightsExactly100(moduleId);
        validateEachRADistributionExactly100(moduleId);
        validateEachActivityInModuleInstrumentsExactly100(moduleId);
    }

    private void validateRAWeightsExactly100(Long moduleId) {
        List<LearningOutcomeRA> ras = learningOutcomeRARepository.findByModuleId(moduleId);
        if (ras.isEmpty()) {
            throw new BusinessValidationException("Module has no RAs configured");
        }

        BigDecimal sum = ras.stream()
                .map(LearningOutcomeRA::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(HUNDRED) != 0) {
            throw new BusinessValidationException("In module " + moduleId + ", sum of RA weights must be exactly 100. Current=" + sum);
        }
    }

    private void validateEachRADistributionExactly100(Long moduleId) {
        List<LearningOutcomeRA> ras = learningOutcomeRARepository.findByModuleId(moduleId);
        for (LearningOutcomeRA ra : ras) {
            BigDecimal sum = utraLinkRepository.findByLearningOutcomeId(ra.getId()).stream()
                    .map(link -> link.getPercent())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(HUNDRED) != 0) {
                throw new BusinessValidationException(
                        "For RA " + ra.getId() + " (" + ra.getCode() + "), sum of UT-RA percentages must be exactly 100. Current=" + sum
                );
            }
        }
    }

    private void validateEachActivityInModuleInstrumentsExactly100(Long moduleId) {
        List<Activity> activities = activityRepository.findByModuleId(moduleId);
        if (activities.isEmpty()) {
            throw new BusinessValidationException("Module has no activities/UTs configured");
        }

        for (Activity activity : activities) {
            BigDecimal sum = instrumentRepository.findByActivityId(activity.getId()).stream()
                    .map(Instrument::getWeightPercent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(HUNDRED) != 0) {
                throw new BusinessValidationException(
                        "For activity " + activity.getId() + ", sum of instrument weights must be exactly 100. Current=" + sum
                );
            }
        }
    }

    public void validateUTBelongsToModule(Long moduleId, Long utId) {
        boolean exists = teachingUnitUTRepository.findByModuleId(moduleId).stream()
                .anyMatch(ut -> ut.getId().equals(utId));
        if (!exists) {
            throw new BusinessValidationException("UT " + utId + " does not belong to module " + moduleId);
        }
    }
}

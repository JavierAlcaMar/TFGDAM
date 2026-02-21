package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.InstrumentExerciseGrade;
import com.sara.tfgdam.domain.entity.InstrumentExerciseWeight;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.dto.ExerciseGradeEntryRequest;
import com.sara.tfgdam.dto.GradeBatchRequest;
import com.sara.tfgdam.dto.GradeEntryRequest;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.InstrumentExerciseGradeRepository;
import com.sara.tfgdam.repository.InstrumentExerciseWeightRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.validation.ConfigurationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final InstrumentExerciseGradeRepository instrumentExerciseGradeRepository;
    private final InstrumentExerciseWeightRepository instrumentExerciseWeightRepository;
    private final StudentRepository studentRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final ConfigurationValidator configurationValidator;

    @Transactional
    public List<Grade> upsertGrades(GradeBatchRequest request) {
        Set<Long> moduleIdsToValidate = new HashSet<>();
        List<Grade> results = new ArrayList<>();

        for (GradeEntryRequest entry : coalesceGradeEntries(request.getGrades())) {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + entry.getStudentId()));
            Instrument instrument = instrumentRepository.findById(entry.getInstrumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + entry.getInstrumentId()));

            Long studentModuleId = student.getModule().getId();
            Long instrumentModuleId = instrument.getActivity().getModule().getId();
            if (!studentModuleId.equals(instrumentModuleId)) {
                throw new BusinessValidationException("Student and instrument must belong to the same module");
            }

            if (instrumentRARepository.findByInstrumentId(instrument.getId()).isEmpty()) {
                throw new BusinessValidationException(
                        "Instrument " + instrument.getId() + " has no RA associations. Link instrument-RA before adding grades."
                );
            }

            List<ExerciseGradeEntryRequest> exerciseEntries = entry.getExerciseGrades();
            validateExerciseEntries(exerciseEntries);

            Grade grade = gradeRepository.findByStudentIdAndInstrumentId(student.getId(), instrument.getId())
                    .orElseGet(() -> Grade.builder()
                            .student(student)
                            .instrument(instrument)
                            .build());

            BigDecimal gradeValue = resolveGradeValue(entry, instrument.getId(), exerciseEntries);
            grade.setGradeValue(gradeValue);
            Grade savedGrade = gradeRepository.save(grade);
            results.add(savedGrade);

            if (exerciseEntries != null) {
                replaceExerciseGrades(student, instrument, exerciseEntries);
            }
            moduleIdsToValidate.add(studentModuleId);
        }

        for (Long moduleId : moduleIdsToValidate) {
            configurationValidator.validateModuleReadyForCalculations(moduleId);
        }

        return results;
    }

    private BigDecimal resolveGradeValue(GradeEntryRequest entry,
                                         Long instrumentId,
                                         List<ExerciseGradeEntryRequest> exerciseEntries) {
        if (exerciseEntries != null && !exerciseEntries.isEmpty()) {
            return computeGradeFromExercises(instrumentId, exerciseEntries, entry.getGradeValue());
        }
        if (entry.getGradeValue() == null) {
            throw new BusinessValidationException("gradeValue is required when exerciseGrades is empty");
        }
        return entry.getGradeValue().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeGradeFromExercises(Long instrumentId,
                                                 List<ExerciseGradeEntryRequest> exerciseEntries,
                                                 BigDecimal fallbackGradeValue) {
        Map<Integer, BigDecimal> gradeByExercise = new LinkedHashMap<>();
        for (ExerciseGradeEntryRequest item : exerciseEntries) {
            gradeByExercise.put(item.getExerciseIndex(), item.getGradeValue());
        }

        List<InstrumentExerciseWeight> weights = instrumentExerciseWeightRepository.findByInstrumentId(instrumentId).stream()
                .sorted(Comparator.comparing(InstrumentExerciseWeight::getExerciseIndex))
                .toList();

        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (InstrumentExerciseWeight weight : weights) {
            BigDecimal weightPercent = weight.getWeightPercent();
            if (weightPercent == null || weightPercent.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal grade = gradeByExercise.getOrDefault(weight.getExerciseIndex(), BigDecimal.ZERO);
            weighted = weighted.add(grade.multiply(weightPercent));
            totalWeight = totalWeight.add(weightPercent);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            return weighted.divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        if (fallbackGradeValue != null) {
            return fallbackGradeValue.setScale(2, RoundingMode.HALF_UP);
        }

        throw new BusinessValidationException(
                "Cannot compute grade from exercises for instrument " + instrumentId + ": no exercise weights configured"
        );
    }

    private void replaceExerciseGrades(Student student,
                                       Instrument instrument,
                                       List<ExerciseGradeEntryRequest> exerciseEntries) {
        Map<Integer, ExerciseGradeEntryRequest> uniqueByIndex = new LinkedHashMap<>();
        for (ExerciseGradeEntryRequest entry : exerciseEntries) {
            uniqueByIndex.put(entry.getExerciseIndex(), entry);
        }

        instrumentExerciseGradeRepository.deleteByStudent_IdAndInstrument_Id(student.getId(), instrument.getId());
        instrumentExerciseGradeRepository.flush();

        if (uniqueByIndex.isEmpty()) {
            return;
        }

        List<InstrumentExerciseGrade> entities = new ArrayList<>();
        for (ExerciseGradeEntryRequest entry : uniqueByIndex.values()) {
            entities.add(InstrumentExerciseGrade.builder()
                    .student(student)
                    .instrument(instrument)
                    .exerciseIndex(entry.getExerciseIndex())
                    .gradeValue(entry.getGradeValue().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        instrumentExerciseGradeRepository.saveAll(entities);
    }

    private void validateExerciseEntries(List<ExerciseGradeEntryRequest> exerciseEntries) {
        if (exerciseEntries == null || exerciseEntries.isEmpty()) {
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (ExerciseGradeEntryRequest entry : exerciseEntries) {
            if (!seen.add(entry.getExerciseIndex())) {
                throw new BusinessValidationException(
                        "Duplicated exerciseIndex in exerciseGrades: " + entry.getExerciseIndex()
                );
            }
        }
    }

    private List<GradeEntryRequest> coalesceGradeEntries(List<GradeEntryRequest> entries) {
        Map<String, GradeEntryRequest> byKey = new LinkedHashMap<>();
        for (GradeEntryRequest entry : entries) {
            String key = entry.getStudentId() + "-" + entry.getInstrumentId();
            GradeEntryRequest current = byKey.get(key);
            if (current == null) {
                byKey.put(key, entry);
                continue;
            }

            if (entry.getGradeValue() != null) {
                current.setGradeValue(entry.getGradeValue());
            }
            if (entry.getExerciseGrades() != null) {
                current.setExerciseGrades(entry.getExerciseGrades());
            }
        }
        return new ArrayList<>(byKey.values());
    }
}

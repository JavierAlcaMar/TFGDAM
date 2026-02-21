package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.InstrumentExerciseGrade;
import com.sara.tfgdam.domain.entity.InstrumentExerciseWeight;
import com.sara.tfgdam.dto.ModulePreviewResponse;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.InstrumentExerciseGradeRepository;
import com.sara.tfgdam.repository.InstrumentExerciseWeightRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.repository.UTRALinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModulePreviewService {

    private final CourseModuleRepository courseModuleRepository;
    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;
    private final UTRALinkRepository utraLinkRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final InstrumentExerciseWeightRepository instrumentExerciseWeightRepository;
    private final InstrumentExerciseGradeRepository instrumentExerciseGradeRepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;

    @Transactional(readOnly = true)
    public ModulePreviewResponse getPreview(Long moduleId) {
        var module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));

        var ras = learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(ra -> ra.getCode().toLowerCase()))
                .toList();

        var uts = teachingUnitUTRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(item -> item.getId()))
                .toList();
        List<Long> utIds = uts.stream().map(item -> item.getId()).toList();

        var utRaLinks = utIds.isEmpty() ? List.<com.sara.tfgdam.domain.entity.UTRALink>of()
                : utraLinkRepository.findByTeachingUnitIdIn(utIds).stream()
                .sorted(Comparator
                        .comparing((com.sara.tfgdam.domain.entity.UTRALink item) -> item.getTeachingUnit().getId())
                        .thenComparing(item -> item.getLearningOutcome().getId()))
                .toList();

        var activities = activityRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator
                        .comparing((com.sara.tfgdam.domain.entity.Activity item) -> item.getTeachingUnit().getId())
                        .thenComparing(item -> item.getId()))
                .toList();
        List<Long> activityIds = activities.stream().map(item -> item.getId()).toList();

        var instruments = activityIds.isEmpty() ? List.<com.sara.tfgdam.domain.entity.Instrument>of()
                : instrumentRepository.findByActivityIdIn(activityIds).stream()
                .sorted(Comparator
                        .comparing((com.sara.tfgdam.domain.entity.Instrument item) -> item.getActivity().getId())
                        .thenComparing(item -> item.getId()))
                .toList();
        List<Long> instrumentIds = instruments.stream().map(item -> item.getId()).toList();

        Map<Long, List<Long>> raIdsByInstrumentId = new HashMap<>();
        if (!instrumentIds.isEmpty()) {
            instrumentRARepository.findByInstrumentIdIn(instrumentIds).forEach(link -> {
                raIdsByInstrumentId.computeIfAbsent(link.getInstrument().getId(), ignored -> new java.util.ArrayList<>())
                        .add(link.getLearningOutcome().getId());
            });
            raIdsByInstrumentId.values()
                    .forEach(ids -> ids.sort(Comparator.naturalOrder()));
        }

        Map<Long, List<ModulePreviewResponse.ExerciseWeightItem>> exerciseWeightsByInstrumentId = new HashMap<>();
        if (!instrumentIds.isEmpty()) {
            Map<Long, List<InstrumentExerciseWeight>> weightsByInstrument = instrumentExerciseWeightRepository
                    .findByInstrumentIdIn(instrumentIds).stream()
                    .collect(Collectors.groupingBy(item -> item.getInstrument().getId()));

            for (Long instrumentId : instrumentIds) {
                List<ModulePreviewResponse.ExerciseWeightItem> weights = weightsByInstrument
                        .getOrDefault(instrumentId, List.of()).stream()
                        .sorted(Comparator.comparing(InstrumentExerciseWeight::getExerciseIndex))
                        .map(item -> ModulePreviewResponse.ExerciseWeightItem.builder()
                                .exerciseIndex(item.getExerciseIndex())
                                .weightPercent(item.getWeightPercent())
                                .build())
                        .toList();
                exerciseWeightsByInstrumentId.put(instrumentId, weights);
            }
        }

        var students = studentRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(item -> item.getStudentCode().toLowerCase()))
                .toList();
        List<Long> studentIds = students.stream().map(item -> item.getId()).toList();

        List<Grade> grades = studentIds.isEmpty() ? List.of()
                : gradeRepository.findByStudentIdIn(studentIds).stream()
                .sorted(Comparator
                        .comparing((Grade item) -> item.getStudent().getId())
                        .thenComparing(item -> item.getInstrument().getId()))
                .toList();

        Map<String, List<ModulePreviewResponse.ExerciseGradeItem>> exerciseGradesByKey = new HashMap<>();
        if (!studentIds.isEmpty()) {
            Map<String, List<InstrumentExerciseGrade>> grouped = instrumentExerciseGradeRepository
                    .findByStudent_IdIn(studentIds).stream()
                    .collect(Collectors.groupingBy(item ->
                            item.getStudent().getId() + "-" + item.getInstrument().getId()));

            grouped.forEach((key, list) -> exerciseGradesByKey.put(
                    key,
                    list.stream()
                            .sorted(Comparator.comparing(InstrumentExerciseGrade::getExerciseIndex))
                            .map(item -> ModulePreviewResponse.ExerciseGradeItem.builder()
                                    .exerciseIndex(item.getExerciseIndex())
                                    .gradeValue(item.getGradeValue())
                                    .build())
                            .toList()
            ));
        }

        return ModulePreviewResponse.builder()
                .moduleId(module.getId())
                .moduleName(module.getName())
                .academicYear(module.getAcademicYear())
                .teacherName(module.getTeacher() != null ? module.getTeacher().getFullName() : null)
                .ras(ras.stream()
                        .map(item -> ModulePreviewResponse.RAItem.builder()
                                .id(item.getId())
                                .code(item.getCode())
                                .name(item.getName())
                                .weightPercent(item.getWeightPercent())
                                .build())
                        .toList())
                .uts(uts.stream()
                        .map(item -> ModulePreviewResponse.UTItem.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .evaluationPeriod(item.getEvaluationPeriod())
                                .build())
                        .toList())
                .utRaLinks(utRaLinks.stream()
                        .map(item -> ModulePreviewResponse.UTRALinkItem.builder()
                                .id(item.getId())
                                .utId(item.getTeachingUnit().getId())
                                .raId(item.getLearningOutcome().getId())
                                .percent(item.getPercent())
                                .build())
                        .toList())
                .activities(activities.stream()
                        .map(item -> ModulePreviewResponse.ActivityItem.builder()
                                .id(item.getId())
                                .utId(item.getTeachingUnit().getId())
                                .evaluationPeriod(item.getTeachingUnit().getEvaluationPeriod())
                                .name(item.getName())
                                .build())
                        .toList())
                .instruments(instruments.stream()
                        .map(item -> ModulePreviewResponse.InstrumentItem.builder()
                                .id(item.getId())
                                .activityId(item.getActivity().getId())
                                .utId(item.getActivity().getTeachingUnit().getId())
                                .name(item.getName())
                                .weightPercent(item.getWeightPercent())
                                .raIds(raIdsByInstrumentId.getOrDefault(item.getId(), List.of()))
                                .exerciseWeights(exerciseWeightsByInstrumentId.getOrDefault(item.getId(), List.of()))
                                .build())
                        .toList())
                .students(students.stream()
                        .map(item -> ModulePreviewResponse.StudentItem.builder()
                                .id(item.getId())
                                .studentCode(item.getStudentCode())
                                .fullName(item.getFullName())
                                .build())
                        .toList())
                .grades(grades.stream()
                        .map(item -> ModulePreviewResponse.GradeItem.builder()
                                .id(item.getId())
                                .studentId(item.getStudent().getId())
                                .instrumentId(item.getInstrument().getId())
                                .gradeValue(item.getGradeValue())
                                .exerciseGrades(exerciseGradesByKey.getOrDefault(
                                        item.getStudent().getId() + "-" + item.getInstrument().getId(),
                                        List.of()
                                ))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

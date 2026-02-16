package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Activity;
import com.sara.tfgdam.domain.entity.CourseModule;
import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.InstrumentRA;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import com.sara.tfgdam.domain.entity.UTRALink;
import com.sara.tfgdam.dto.ActivityGradeDto;
import com.sara.tfgdam.dto.EvaluationGradeDto;
import com.sara.tfgdam.dto.ModuleEvaluationReportResponse;
import com.sara.tfgdam.dto.ModuleFinalReportResponse;
import com.sara.tfgdam.dto.RAGradeDto;
import com.sara.tfgdam.dto.StudentEvaluationReportRow;
import com.sara.tfgdam.dto.StudentFinalReportRow;
import com.sara.tfgdam.dto.StudentReportResponse;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ONE = new BigDecimal("1.00");
    private static final BigDecimal FIVE = new BigDecimal("5.00");

    private final CourseModuleRepository courseModuleRepository;
    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;
    private final UTRALinkRepository utraLinkRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final ConfigurationValidator configurationValidator;

    @Transactional(readOnly = true)
    public StudentReportResponse getStudentReport(Long studentId, Long moduleId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (!student.getModule().getId().equals(moduleId)) {
            throw new BusinessValidationException("Student does not belong to moduleId=" + moduleId);
        }

        ModuleContext context = buildContext(moduleId);

        Map<Long, BigDecimal> gradeByInstrumentId = gradeRepository.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(g -> g.getInstrument().getId(), Grade::getGradeValue));

        StudentComputation computation = computeForStudent(context, gradeByInstrumentId);

        return StudentReportResponse.builder()
                .studentId(student.getId())
                .moduleId(moduleId)
                .activityGrades(buildActivityGrades(context, computation))
                .raGrades(buildRaGrades(context, computation))
                .evaluationGrades(buildEvaluationGrades(context, computation))
                .finalGrade(computation.finalGrade)
                .build();
    }

    @Transactional(readOnly = true)
    public ModuleEvaluationReportResponse getModuleEvaluationReport(Long moduleId, Integer evaluationPeriod) {
        ModuleContext context = buildContext(moduleId);

        if (!context.evaluationPeriods.contains(evaluationPeriod)) {
            throw new BusinessValidationException("Evaluation period not configured in module: " + evaluationPeriod);
        }

        List<Student> students = studentRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(Student::getStudentCode))
                .toList();

        Map<Long, Map<Long, BigDecimal>> gradesByStudent = buildGradesByStudent(students);

        List<StudentEvaluationReportRow> rows = new ArrayList<>();

        for (Student student : students) {
            StudentComputation computation = computeForStudent(
                    context,
                    gradesByStudent.getOrDefault(student.getId(), Map.of())
            );

            EvaluationResult result = computation.evaluationResults.get(evaluationPeriod);
            rows.add(StudentEvaluationReportRow.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getFullName())
                    .numericGrade(result != null ? result.numericGrade : ZERO)
                    .suggestedBulletinGrade(result != null ? result.suggestedBulletinGrade : 1)
                    .allRAsPassed(result != null && result.allRAsPassed)
                    .build());
        }

        return ModuleEvaluationReportResponse.builder()
                .moduleId(moduleId)
                .evaluationPeriod(evaluationPeriod)
                .students(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public ModuleFinalReportResponse getModuleFinalReport(Long moduleId) {
        ModuleContext context = buildContext(moduleId);

        List<Student> students = studentRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(Student::getStudentCode))
                .toList();

        Map<Long, Map<Long, BigDecimal>> gradesByStudent = buildGradesByStudent(students);

        List<StudentFinalReportRow> rows = new ArrayList<>();

        for (Student student : students) {
            StudentComputation computation = computeForStudent(
                    context,
                    gradesByStudent.getOrDefault(student.getId(), Map.of())
            );

            rows.add(StudentFinalReportRow.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getFullName())
                    .finalGrade(computation.finalGrade)
                    .build());
        }

        return ModuleFinalReportResponse.builder()
                .moduleId(moduleId)
                .students(rows)
                .build();
    }

    int calculateSuggestedBulletinGrade(BigDecimal numericGrade, boolean allRAsPassed) {
        if (numericGrade.compareTo(ONE) < 0) {
            return 1;
        }

        if (numericGrade.compareTo(FIVE) < 0) {
            return numericGrade.setScale(0, RoundingMode.DOWN).intValue();
        }

        if (!allRAsPassed) {
            return 4;
        }

        return numericGrade.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private ModuleContext buildContext(Long moduleId) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));

        configurationValidator.validateModuleReadyForCalculations(moduleId);

        List<LearningOutcomeRA> ras = learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(LearningOutcomeRA::getCode))
                .toList();
        List<TeachingUnitUT> uts = teachingUnitUTRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(TeachingUnitUT::getId))
                .toList();

        List<Long> utIds = uts.stream().map(TeachingUnitUT::getId).toList();
        List<UTRALink> utRaLinks = utIds.isEmpty() ? List.of() : utraLinkRepository.findByTeachingUnitIdIn(utIds);

        List<Activity> activities = activityRepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(a -> a.getTeachingUnit().getId()))
                .toList();
        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        List<Instrument> instruments = activityIds.isEmpty() ? List.of() : instrumentRepository.findByActivityIdIn(activityIds);
        List<Long> instrumentIds = instruments.stream().map(Instrument::getId).toList();
        List<InstrumentRA> instrumentRAs = instrumentIds.isEmpty() ? List.of() : instrumentRARepository.findByInstrumentIdIn(instrumentIds);

        return new ModuleContext(module, ras, uts, utRaLinks, activities, instruments, instrumentRAs);
    }

    private Map<Long, Map<Long, BigDecimal>> buildGradesByStudent(List<Student> students) {
        if (students.isEmpty()) {
            return Map.of();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        List<Grade> grades = gradeRepository.findByStudentIdIn(studentIds);

        Map<Long, Map<Long, BigDecimal>> byStudent = new HashMap<>();
        for (Grade grade : grades) {
            byStudent.computeIfAbsent(grade.getStudent().getId(), k -> new HashMap<>())
                    .put(grade.getInstrument().getId(), grade.getGradeValue());
        }
        return byStudent;
    }

    private StudentComputation computeForStudent(ModuleContext context, Map<Long, BigDecimal> gradeByInstrumentId) {
        Map<Long, BigDecimal> activityGrades = new LinkedHashMap<>();

        for (Activity activity : context.activities) {
            BigDecimal grade = calculateActivityGrade(activity.getId(), context, gradeByInstrumentId);
            activityGrades.put(activity.getId(), grade);
        }

        Map<UtRaKey, BigDecimal> utRaStudentGrades = calculateUtRaGrades(context, gradeByInstrumentId);

        Map<Long, BigDecimal> raGlobalGrades = new LinkedHashMap<>();
        for (LearningOutcomeRA ra : context.ras) {
            BigDecimal grade = ZERO;
            for (UTRALink link : context.utRaLinksByRaId.getOrDefault(ra.getId(), List.of())) {
                BigDecimal utRaGrade = utRaStudentGrades.getOrDefault(
                        new UtRaKey(link.getTeachingUnit().getId(), ra.getId()),
                        ZERO
                );
                grade = grade.add(utRaGrade.multiply(link.getPercent()).divide(HUNDRED, 8, RoundingMode.HALF_UP));
            }
            raGlobalGrades.put(ra.getId(), scale(grade));
        }

        BigDecimal finalGrade = ZERO;
        for (LearningOutcomeRA ra : context.ras) {
            BigDecimal raGrade = raGlobalGrades.getOrDefault(ra.getId(), ZERO);
            finalGrade = finalGrade.add(raGrade.multiply(ra.getWeightPercent()).divide(HUNDRED, 8, RoundingMode.HALF_UP));
        }
        finalGrade = scale(finalGrade);

        Map<Integer, EvaluationResult> evaluationResults = calculateEvaluationResults(context, utRaStudentGrades);

        return new StudentComputation(activityGrades, raGlobalGrades, evaluationResults, finalGrade);
    }

    private BigDecimal calculateActivityGrade(Long activityId,
                                              ModuleContext context,
                                              Map<Long, BigDecimal> gradeByInstrumentId) {
        BigDecimal result = ZERO;
        for (Instrument instrument : context.instrumentsByActivityId.getOrDefault(activityId, List.of())) {
            BigDecimal instrumentGrade = gradeByInstrumentId.getOrDefault(instrument.getId(), ZERO);
            result = result.add(
                    instrumentGrade.multiply(instrument.getWeightPercent())
                            .divide(HUNDRED, 8, RoundingMode.HALF_UP)
            );
        }
        return scale(result);
    }

    private Map<UtRaKey, BigDecimal> calculateUtRaGrades(ModuleContext context,
                                                          Map<Long, BigDecimal> gradeByInstrumentId) {
        Map<UtRaKey, BigDecimal> result = new HashMap<>();

        for (UTRALink link : context.utRaLinks) {
            Long utId = link.getTeachingUnit().getId();
            Long raId = link.getLearningOutcome().getId();
            Activity activity = context.activityByUtId.get(utId);

            if (activity == null) {
                result.put(new UtRaKey(utId, raId), ZERO);
                continue;
            }

            BigDecimal numerator = ZERO;
            BigDecimal denominator = ZERO;

            for (Instrument instrument : context.instrumentsByActivityId.getOrDefault(activity.getId(), List.of())) {
                Set<Long> linkedRaIds = context.raIdsByInstrumentId.getOrDefault(instrument.getId(), Set.of());
                if (!linkedRaIds.contains(raId)) {
                    continue;
                }

                BigDecimal instrumentGrade = gradeByInstrumentId.getOrDefault(instrument.getId(), ZERO);
                BigDecimal weight = instrument.getWeightPercent();

                numerator = numerator.add(instrumentGrade.multiply(weight));
                denominator = denominator.add(weight);
            }

            BigDecimal utRaGrade;
            if (denominator.compareTo(ZERO) == 0) {
                utRaGrade = ZERO;
            } else {
                utRaGrade = numerator.divide(denominator, 8, RoundingMode.HALF_UP);
            }

            result.put(new UtRaKey(utId, raId), scale(utRaGrade));
        }

        return result;
    }

    private Map<Integer, EvaluationResult> calculateEvaluationResults(ModuleContext context,
                                                                      Map<UtRaKey, BigDecimal> utRaStudentGrades) {
        Map<Integer, EvaluationResult> results = new LinkedHashMap<>();

        for (Integer evaluationPeriod : context.evaluationPeriods) {
            // Criterion chosen for evaluation grade:
            // 1) For each RA present in this evaluation, compute its RA-in-evaluation grade
            //    using only UTs from this evaluation and normalizing by the RA UT-RA percentages
            //    present in this evaluation.
            // 2) Aggregate those RA-in-evaluation grades weighted by global RA weights and
            //    normalized by the sum of weights of RAs present in this evaluation.
            Set<Long> raIdsInEvaluation = context.raIdsByEvaluation.getOrDefault(evaluationPeriod, Set.of());

            BigDecimal weightedSum = ZERO;
            BigDecimal totalRaWeight = ZERO;
            boolean allPassed = true;

            for (Long raId : raIdsInEvaluation) {
                LearningOutcomeRA ra = context.raById.get(raId);
                if (ra == null) {
                    continue;
                }

                List<UTRALink> linksInEval = context.utRaLinksByEvaluationAndRa
                        .getOrDefault(evaluationPeriod, Map.of())
                        .getOrDefault(raId, List.of());

                BigDecimal percentSum = linksInEval.stream()
                        .map(UTRALink::getPercent)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal raEvalGrade = ZERO;
                if (percentSum.compareTo(ZERO) > 0) {
                    BigDecimal numerator = ZERO;
                    for (UTRALink link : linksInEval) {
                        BigDecimal utRaGrade = utRaStudentGrades.getOrDefault(
                                new UtRaKey(link.getTeachingUnit().getId(), raId),
                                ZERO
                        );
                        numerator = numerator.add(utRaGrade.multiply(link.getPercent()));
                    }
                    raEvalGrade = numerator.divide(percentSum, 8, RoundingMode.HALF_UP);
                }

                if (raEvalGrade.compareTo(FIVE) < 0) {
                    allPassed = false;
                }

                weightedSum = weightedSum.add(raEvalGrade.multiply(ra.getWeightPercent()));
                totalRaWeight = totalRaWeight.add(ra.getWeightPercent());
            }

            BigDecimal numericGrade = totalRaWeight.compareTo(ZERO) > 0
                    ? weightedSum.divide(totalRaWeight, 8, RoundingMode.HALF_UP)
                    : ZERO;
            numericGrade = scale(numericGrade);

            int suggested = calculateSuggestedBulletinGrade(numericGrade, allPassed);

            results.put(evaluationPeriod, new EvaluationResult(numericGrade, suggested, allPassed));
        }

        return results;
    }

    private List<ActivityGradeDto> buildActivityGrades(ModuleContext context, StudentComputation computation) {
        List<ActivityGradeDto> result = new ArrayList<>();

        for (Activity activity : context.activities) {
            result.add(ActivityGradeDto.builder()
                    .activityId(activity.getId())
                    .utId(activity.getTeachingUnit().getId())
                    .activityName(activity.getName())
                    .evaluationPeriod(activity.getTeachingUnit().getEvaluationPeriod())
                    .grade(computation.activityGrades.getOrDefault(activity.getId(), ZERO))
                    .build());
        }

        return result;
    }

    private List<RAGradeDto> buildRaGrades(ModuleContext context, StudentComputation computation) {
        List<RAGradeDto> result = new ArrayList<>();

        for (LearningOutcomeRA ra : context.ras) {
            result.add(RAGradeDto.builder()
                    .raId(ra.getId())
                    .raCode(ra.getCode())
                    .raName(ra.getName())
                    .grade(computation.raGlobalGrades.getOrDefault(ra.getId(), ZERO))
                    .build());
        }

        return result;
    }

    private List<EvaluationGradeDto> buildEvaluationGrades(ModuleContext context, StudentComputation computation) {
        List<EvaluationGradeDto> result = new ArrayList<>();

        for (Integer period : context.evaluationPeriods) {
            EvaluationResult r = computation.evaluationResults.get(period);
            result.add(EvaluationGradeDto.builder()
                    .evaluationPeriod(period)
                    .numericGrade(r != null ? r.numericGrade : ZERO)
                    .suggestedBulletinGrade(r != null ? r.suggestedBulletinGrade : 1)
                    .allRAsPassed(r != null && r.allRAsPassed)
                    .build());
        }

        return result;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static final class ModuleContext {
        private final CourseModule module;
        private final List<LearningOutcomeRA> ras;
        private final List<TeachingUnitUT> uts;
        private final List<UTRALink> utRaLinks;
        private final List<Activity> activities;
        private final List<Instrument> instruments;
        private final List<InstrumentRA> instrumentRAs;

        private final Map<Long, LearningOutcomeRA> raById;
        private final Map<Long, List<UTRALink>> utRaLinksByRaId;
        private final Map<Long, Activity> activityByUtId;
        private final Map<Long, List<Instrument>> instrumentsByActivityId;
        private final Map<Long, Set<Long>> raIdsByInstrumentId;
        private final Set<Integer> evaluationPeriods;
        private final Map<Integer, Set<Long>> raIdsByEvaluation;
        private final Map<Integer, Map<Long, List<UTRALink>>> utRaLinksByEvaluationAndRa;

        private ModuleContext(CourseModule module,
                              List<LearningOutcomeRA> ras,
                              List<TeachingUnitUT> uts,
                              List<UTRALink> utRaLinks,
                              List<Activity> activities,
                              List<Instrument> instruments,
                              List<InstrumentRA> instrumentRAs) {
            this.module = module;
            this.ras = ras;
            this.uts = uts;
            this.utRaLinks = utRaLinks;
            this.activities = activities;
            this.instruments = instruments;
            this.instrumentRAs = instrumentRAs;

            this.raById = ras.stream().collect(Collectors.toMap(LearningOutcomeRA::getId, ra -> ra));

            this.utRaLinksByRaId = utRaLinks.stream()
                    .collect(Collectors.groupingBy(link -> link.getLearningOutcome().getId()));

            this.activityByUtId = activities.stream()
                    .collect(Collectors.toMap(activity -> activity.getTeachingUnit().getId(), activity -> activity));

            this.instrumentsByActivityId = instruments.stream()
                    .collect(Collectors.groupingBy(instrument -> instrument.getActivity().getId()));

            this.raIdsByInstrumentId = new HashMap<>();
            for (InstrumentRA link : instrumentRAs) {
                this.raIdsByInstrumentId
                        .computeIfAbsent(link.getInstrument().getId(), k -> new LinkedHashSet<>())
                        .add(link.getLearningOutcome().getId());
            }

            Map<Long, Integer> utEvaluationMap = uts.stream()
                    .collect(Collectors.toMap(TeachingUnitUT::getId, TeachingUnitUT::getEvaluationPeriod));

            this.evaluationPeriods = new TreeSet<>(uts.stream()
                    .map(TeachingUnitUT::getEvaluationPeriod)
                    .collect(Collectors.toSet()));

            this.raIdsByEvaluation = new HashMap<>();
            this.utRaLinksByEvaluationAndRa = new HashMap<>();

            for (UTRALink link : utRaLinks) {
                Integer evaluation = utEvaluationMap.get(link.getTeachingUnit().getId());
                if (evaluation == null) {
                    continue;
                }

                this.raIdsByEvaluation
                        .computeIfAbsent(evaluation, k -> new HashSet<>())
                        .add(link.getLearningOutcome().getId());

                this.utRaLinksByEvaluationAndRa
                        .computeIfAbsent(evaluation, k -> new HashMap<>())
                        .computeIfAbsent(link.getLearningOutcome().getId(), k -> new ArrayList<>())
                        .add(link);
            }
        }
    }

    private record UtRaKey(Long utId, Long raId) {
    }

    private static final class StudentComputation {
        private final Map<Long, BigDecimal> activityGrades;
        private final Map<Long, BigDecimal> raGlobalGrades;
        private final Map<Integer, EvaluationResult> evaluationResults;
        private final BigDecimal finalGrade;

        private StudentComputation(Map<Long, BigDecimal> activityGrades,
                                   Map<Long, BigDecimal> raGlobalGrades,
                                   Map<Integer, EvaluationResult> evaluationResults,
                                   BigDecimal finalGrade) {
            this.activityGrades = activityGrades;
            this.raGlobalGrades = raGlobalGrades;
            this.evaluationResults = evaluationResults;
            this.finalGrade = finalGrade;
        }
    }

    private static final class EvaluationResult {
        private final BigDecimal numericGrade;
        private final int suggestedBulletinGrade;
        private final boolean allRAsPassed;

        private EvaluationResult(BigDecimal numericGrade, int suggestedBulletinGrade, boolean allRAsPassed) {
            this.numericGrade = numericGrade;
            this.suggestedBulletinGrade = suggestedBulletinGrade;
            this.allRAsPassed = allRAsPassed;
        }
    }
}

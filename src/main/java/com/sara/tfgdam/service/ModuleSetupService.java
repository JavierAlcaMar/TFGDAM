package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Activity;
import com.sara.tfgdam.domain.entity.CourseModule;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.InstrumentRA;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.domain.entity.Teacher;
import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import com.sara.tfgdam.domain.entity.UTRALink;
import com.sara.tfgdam.dto.CreateInstrumentRequest;
import com.sara.tfgdam.dto.CreateModuleRequest;
import com.sara.tfgdam.dto.CreateRARequest;
import com.sara.tfgdam.dto.CreateStudentRequest;
import com.sara.tfgdam.dto.CreateTeacherRequest;
import com.sara.tfgdam.dto.CreateUTRequest;
import com.sara.tfgdam.dto.ImportRAItemDto;
import com.sara.tfgdam.dto.ImportRAsConfirmRequest;
import com.sara.tfgdam.dto.InstrumentRAResponse;
import com.sara.tfgdam.dto.PatchInstrumentRARequest;
import com.sara.tfgdam.dto.PatchInstrumentRequest;
import com.sara.tfgdam.dto.PatchRARequest;
import com.sara.tfgdam.dto.PatchUTRALinkRequest;
import com.sara.tfgdam.dto.PatchUTRequest;
import com.sara.tfgdam.dto.SetInstrumentRAsRequest;
import com.sara.tfgdam.dto.UpdateInstrumentRequest;
import com.sara.tfgdam.dto.UpdateRARequest;
import com.sara.tfgdam.dto.UpdateUTRALinkRequest;
import com.sara.tfgdam.dto.UpdateUTRequest;
import com.sara.tfgdam.dto.UpsertUTRALinkRequest;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.ImportJobRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.repository.StudentEvaluationOverrideRepository;
import com.sara.tfgdam.repository.TeacherRepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.repository.UTRALinkRepository;
import com.sara.tfgdam.validation.ConfigurationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModuleSetupService {

    private final TeacherRepository teacherRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;
    private final UTRALinkRepository utraLinkRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final StudentRepository studentRepository;
    private final StudentEvaluationOverrideRepository studentEvaluationOverrideRepository;
    private final GradeRepository gradeRepository;
    private final ImportJobRepository importJobRepository;
    private final ConfigurationValidator configurationValidator;

    @Transactional
    public Teacher createTeacher(CreateTeacherRequest request) {
        Teacher teacher = Teacher.builder()
                .fullName(request.getFullName().trim())
                .build();
        return teacherRepository.save(teacher);
    }

    @Transactional
    public CourseModule createModule(CreateModuleRequest request) {
        Teacher teacher = resolveTeacher(request.getTeacherId(), request.getTeacherName());

        CourseModule module = CourseModule.builder()
                .name(request.getName().trim())
                .academicYear(request.getAcademicYear())
                .teacher(teacher)
                .build();
        return courseModuleRepository.save(module);
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        CourseModule module = getModule(moduleId);

        List<Student> students = studentRepository.findByModuleId(moduleId);
        List<Long> studentIds = students.stream().map(Student::getId).toList();

        List<Activity> activities = activityRepository.findByModuleId(moduleId);
        List<Long> activityIds = activities.stream().map(Activity::getId).toList();

        List<Instrument> instruments = activityIds.isEmpty()
                ? List.of()
                : instrumentRepository.findByActivityIdIn(activityIds);
        List<Long> instrumentIds = instruments.stream().map(Instrument::getId).toList();

        List<TeachingUnitUT> uts = teachingUnitUTRepository.findByModuleId(moduleId);
        List<Long> utIds = uts.stream().map(TeachingUnitUT::getId).toList();

        List<Long> raIds = learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .map(LearningOutcomeRA::getId)
                .toList();

        if (!instrumentIds.isEmpty()) {
            gradeRepository.deleteByInstrumentIdIn(instrumentIds);
            instrumentRARepository.deleteByInstrumentIdIn(instrumentIds);
            instrumentRepository.deleteAllById(instrumentIds);
        }

        if (!studentIds.isEmpty()) {
            studentEvaluationOverrideRepository.deleteByStudent_Module_Id(moduleId);
            gradeRepository.deleteByStudentIdIn(studentIds);
            studentRepository.deleteAllById(studentIds);
        }

        if (!utIds.isEmpty()) {
            for (Long utId : utIds) {
                utraLinkRepository.deleteByTeachingUnitId(utId);
            }
        }

        if (!activityIds.isEmpty()) {
            activityRepository.deleteAllById(activityIds);
        }

        if (!utIds.isEmpty()) {
            teachingUnitUTRepository.deleteAllById(utIds);
        }

        if (!raIds.isEmpty()) {
            for (Long raId : raIds) {
                instrumentRARepository.deleteByLearningOutcomeId(raId);
            }
            learningOutcomeRARepository.deleteAllById(raIds);
        }

        importJobRepository.deleteByModuleId(moduleId);
        courseModuleRepository.delete(module);
    }

    @Transactional
    public LearningOutcomeRA createRA(Long moduleId, CreateRARequest request) {
        CourseModule module = getModule(moduleId);

        LearningOutcomeRA ra = LearningOutcomeRA.builder()
                .module(module)
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .weightPercent(request.getWeightPercent())
                .build();

        LearningOutcomeRA saved = learningOutcomeRARepository.save(ra);
        configurationValidator.validateRAWeightDoesNotExceed100(moduleId);
        return saved;
    }

    @Transactional
    public LearningOutcomeRA updateRA(Long raId, UpdateRARequest request) {
        LearningOutcomeRA ra = getRA(raId);

        ra.setCode(request.getCode().trim());
        ra.setName(request.getName().trim());
        ra.setWeightPercent(request.getWeightPercent());

        LearningOutcomeRA saved = learningOutcomeRARepository.save(ra);
        configurationValidator.validateRAWeightDoesNotExceed100(ra.getModule().getId());
        return saved;
    }

    @Transactional
    public LearningOutcomeRA patchRA(Long raId, PatchRARequest request) {
        LearningOutcomeRA ra = getRA(raId);

        if (request.getCode() == null && request.getName() == null && request.getWeightPercent() == null) {
            throw new BusinessValidationException("PATCH RA requires at least one field");
        }

        if (request.getCode() != null) {
            ra.setCode(requireNonBlank(request.getCode(), "RA code cannot be blank"));
        }
        if (request.getName() != null) {
            ra.setName(requireNonBlank(request.getName(), "RA name cannot be blank"));
        }
        if (request.getWeightPercent() != null) {
            ra.setWeightPercent(request.getWeightPercent());
        }

        LearningOutcomeRA saved = learningOutcomeRARepository.save(ra);
        configurationValidator.validateRAWeightDoesNotExceed100(ra.getModule().getId());
        return saved;
    }

    @Transactional
    public void deleteRA(Long raId) {
        LearningOutcomeRA ra = getRA(raId);

        utraLinkRepository.deleteByLearningOutcomeId(ra.getId());
        instrumentRARepository.deleteByLearningOutcomeId(ra.getId());
        learningOutcomeRARepository.delete(ra);
    }

    @Transactional(readOnly = true)
    public LearningOutcomeRA getRAById(Long raId) {
        return getRA(raId);
    }

    @Transactional(readOnly = true)
    public List<LearningOutcomeRA> getRAsByModuleId(Long moduleId) {
        getModule(moduleId);
        return learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .sorted(Comparator.comparing(LearningOutcomeRA::getId))
                .toList();
    }

    @Transactional
    public TeachingUnitUT createUT(Long moduleId, CreateUTRequest request) {
        CourseModule module = getModule(moduleId);

        TeachingUnitUT ut = TeachingUnitUT.builder()
                .module(module)
                .name(request.getName().trim())
                .evaluationPeriod(request.getEvaluationPeriod())
                .build();

        TeachingUnitUT savedUt = teachingUnitUTRepository.save(ut);

        Activity activity = Activity.builder()
                .module(module)
                .teachingUnit(savedUt)
                .name(request.getName().trim())
                .build();

        activityRepository.save(activity);
        return savedUt;
    }

    @Transactional
    public TeachingUnitUT updateUT(Long utId, UpdateUTRequest request) {
        TeachingUnitUT ut = getUT(utId);

        ut.setName(request.getName().trim());
        ut.setEvaluationPeriod(request.getEvaluationPeriod());

        TeachingUnitUT savedUt = teachingUnitUTRepository.save(ut);

        activityRepository.findByTeachingUnitId(utId).ifPresent(activity -> {
            activity.setName(request.getName().trim());
            activityRepository.save(activity);
        });

        return savedUt;
    }

    @Transactional
    public TeachingUnitUT patchUT(Long utId, PatchUTRequest request) {
        TeachingUnitUT ut = getUT(utId);

        if (request.getName() == null && request.getEvaluationPeriod() == null) {
            throw new BusinessValidationException("PATCH UT requires at least one field");
        }

        if (request.getName() != null) {
            String normalized = requireNonBlank(request.getName(), "UT name cannot be blank");
            ut.setName(normalized);
            activityRepository.findByTeachingUnitId(utId).ifPresent(activity -> {
                activity.setName(normalized);
                activityRepository.save(activity);
            });
        }

        if (request.getEvaluationPeriod() != null) {
            ut.setEvaluationPeriod(request.getEvaluationPeriod());
        }

        return teachingUnitUTRepository.save(ut);
    }

    @Transactional
    public void deleteUT(Long utId) {
        TeachingUnitUT ut = getUT(utId);

        Activity activity = activityRepository.findByTeachingUnitId(utId)
                .orElse(null);

        if (activity != null) {
            List<Instrument> instruments = instrumentRepository.findByActivityId(activity.getId());
            if (!instruments.isEmpty()) {
                List<Long> instrumentIds = instruments.stream().map(Instrument::getId).toList();
                gradeRepository.deleteByInstrumentIdIn(instrumentIds);
                instrumentRARepository.deleteByInstrumentIdIn(instrumentIds);
                instrumentRepository.deleteAllById(instrumentIds);
            }
            activityRepository.delete(activity);
        }

        utraLinkRepository.deleteByTeachingUnitId(utId);
        teachingUnitUTRepository.delete(ut);
    }

    @Transactional
    public UTRALink upsertUTRALink(Long moduleId, UpsertUTRALinkRequest request) {
        CourseModule module = getModule(moduleId);

        TeachingUnitUT ut = getUT(request.getUtId());
        LearningOutcomeRA ra = getRA(request.getRaId());

        if (!ut.getModule().getId().equals(module.getId()) || !ra.getModule().getId().equals(module.getId())) {
            throw new BusinessValidationException("UT and RA must belong to the module in path");
        }

        UTRALink link = utraLinkRepository.findByTeachingUnitIdAndLearningOutcomeId(ut.getId(), ra.getId())
                .orElseGet(() -> UTRALink.builder().teachingUnit(ut).learningOutcome(ra).build());

        link.setPercent(request.getPercent());

        UTRALink saved = utraLinkRepository.save(link);
        configurationValidator.validateRADistributionDoesNotExceed100(ra.getId());
        return saved;
    }

    @Transactional
    public UTRALink updateUTRALink(Long moduleId, Long linkId, UpdateUTRALinkRequest request) {
        UTRALink link = getUTRALinkInModule(moduleId, linkId);
        link.setPercent(request.getPercent());

        UTRALink saved = utraLinkRepository.save(link);
        configurationValidator.validateRADistributionDoesNotExceed100(link.getLearningOutcome().getId());
        return saved;
    }

    @Transactional
    public UTRALink patchUTRALink(Long moduleId, Long linkId, PatchUTRALinkRequest request) {
        UTRALink link = getUTRALinkInModule(moduleId, linkId);
        if (request.getPercent() == null) {
            throw new BusinessValidationException("PATCH UT-RA requires percent");
        }

        link.setPercent(request.getPercent());
        UTRALink saved = utraLinkRepository.save(link);
        configurationValidator.validateRADistributionDoesNotExceed100(link.getLearningOutcome().getId());
        return saved;
    }

    @Transactional
    public void deleteUTRALink(Long moduleId, Long linkId) {
        UTRALink link = getUTRALinkInModule(moduleId, linkId);
        utraLinkRepository.delete(link);
    }

    @Transactional
    public Instrument createInstrument(Long utId, CreateInstrumentRequest request) {
        Activity activity = getActivityByUT(utId);

        Instrument instrument = Instrument.builder()
                .activity(activity)
                .name(request.getName().trim())
                .weightPercent(request.getWeightPercent())
                .build();

        Instrument saved = instrumentRepository.save(instrument);
        configurationValidator.validateInstrumentWeightsDoNotExceed100(activity.getId());
        return saved;
    }

    @Transactional
    public Instrument updateInstrument(Long instrumentId, UpdateInstrumentRequest request) {
        Instrument instrument = getInstrument(instrumentId);

        instrument.setName(request.getName().trim());
        instrument.setWeightPercent(request.getWeightPercent());

        Instrument saved = instrumentRepository.save(instrument);
        configurationValidator.validateInstrumentWeightsDoNotExceed100(instrument.getActivity().getId());
        return saved;
    }

    @Transactional
    public Instrument patchInstrument(Long instrumentId, PatchInstrumentRequest request) {
        Instrument instrument = getInstrument(instrumentId);

        if (request.getName() == null && request.getWeightPercent() == null) {
            throw new BusinessValidationException("PATCH instrument requires at least one field");
        }

        if (request.getName() != null) {
            instrument.setName(requireNonBlank(request.getName(), "Instrument name cannot be blank"));
        }

        if (request.getWeightPercent() != null) {
            instrument.setWeightPercent(request.getWeightPercent());
        }

        Instrument saved = instrumentRepository.save(instrument);
        configurationValidator.validateInstrumentWeightsDoNotExceed100(instrument.getActivity().getId());
        return saved;
    }

    @Transactional
    public void deleteInstrument(Long instrumentId) {
        Instrument instrument = getInstrument(instrumentId);
        gradeRepository.deleteByInstrumentId(instrumentId);
        instrumentRARepository.deleteByInstrumentId(instrumentId);
        instrumentRepository.delete(instrument);
    }

    @Transactional
    public InstrumentRAResponse setInstrumentRAs(Long instrumentId, SetInstrumentRAsRequest request) {
        Instrument instrument = getInstrument(instrumentId);
        Set<Long> targetRaIds = new LinkedHashSet<>(request.getRaIds());
        return applyInstrumentRASet(instrument, targetRaIds);
    }

    @Transactional
    public InstrumentRAResponse patchInstrumentRAs(Long instrumentId, PatchInstrumentRARequest request) {
        Instrument instrument = getInstrument(instrumentId);

        List<Long> addRaIds = request.getAddRaIds() == null ? List.of() : request.getAddRaIds();
        List<Long> removeRaIds = request.getRemoveRaIds() == null ? List.of() : request.getRemoveRaIds();

        if (addRaIds.isEmpty() && removeRaIds.isEmpty()) {
            throw new BusinessValidationException("PATCH instrument-RA requires addRaIds or removeRaIds");
        }

        Set<Long> currentRaIds = instrumentRARepository.findByInstrumentId(instrumentId).stream()
                .map(link -> link.getLearningOutcome().getId())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        currentRaIds.removeAll(removeRaIds);
        currentRaIds.addAll(addRaIds);

        return applyInstrumentRASet(instrument, currentRaIds);
    }

    @Transactional
    public InstrumentRAResponse deleteInstrumentRA(Long instrumentId, Long raId) {
        Instrument instrument = getInstrument(instrumentId);

        instrumentRARepository.findByInstrumentIdAndLearningOutcomeId(instrumentId, raId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument-RA link not found for instrument=" + instrumentId + " ra=" + raId));

        instrumentRARepository.deleteByInstrumentIdAndLearningOutcomeId(instrumentId, raId);

        return InstrumentRAResponse.builder()
                .instrumentId(instrumentId)
                .utId(instrument.getActivity().getTeachingUnit().getId())
                .raIds(instrumentRARepository.findByInstrumentId(instrumentId).stream()
                        .map(link -> link.getLearningOutcome().getId())
                        .toList())
                .build();
    }

    @Transactional
    public InstrumentRAResponse clearInstrumentRAs(Long instrumentId) {
        Instrument instrument = getInstrument(instrumentId);
        instrumentRARepository.deleteByInstrumentId(instrumentId);

        return InstrumentRAResponse.builder()
                .instrumentId(instrumentId)
                .utId(instrument.getActivity().getTeachingUnit().getId())
                .raIds(List.of())
                .build();
    }

    @Transactional
    public Student createStudent(CreateStudentRequest request) {
        CourseModule module = getModule(request.getModuleId());

        studentRepository.findByModuleIdAndStudentCode(module.getId(), request.getStudentCode().trim())
                .ifPresent(existing -> {
                    throw new BusinessValidationException("Student code already exists in module: " + request.getStudentCode());
                });

        Student student = Student.builder()
                .module(module)
                .studentCode(request.getStudentCode().trim())
                .fullName(request.getFullName().trim())
                .build();

        return studentRepository.save(student);
    }

    @Transactional
    public List<LearningOutcomeRA> importRAs(Long moduleId, ImportRAsConfirmRequest request) {
        CourseModule module = getModule(moduleId);

        BigDecimal existingWeight = learningOutcomeRARepository.findByModuleId(moduleId).stream()
                .map(LearningOutcomeRA::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incomingWeight = request.getRas().stream()
                .map(ImportRAItemDto::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (existingWeight.add(incomingWeight).compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessValidationException(
                    "Cannot import RAs. Current weight is " + existingWeight + " and imported weight is "
                            + incomingWeight + ". Total would exceed 100."
            );
        }

        List<LearningOutcomeRA> created = new ArrayList<>();
        for (ImportRAItemDto item : request.getRas()) {
            LearningOutcomeRA ra = LearningOutcomeRA.builder()
                    .module(module)
                    .code(item.getCode().trim())
                    .name(item.getName().trim())
                    .weightPercent(item.getWeightPercent())
                    .build();
            created.add(learningOutcomeRARepository.save(ra));
        }

        configurationValidator.validateRAWeightDoesNotExceed100(moduleId);
        return created;
    }

    @Transactional(readOnly = true)
    public Long getActivityIdByUT(Long utId) {
        return getActivityByUT(utId).getId();
    }

    private InstrumentRAResponse applyInstrumentRASet(Instrument instrument, Set<Long> targetRaIds) {
        Long instrumentId = instrument.getId();
        Set<Long> currentRaIds = instrumentRARepository.findByInstrumentId(instrumentId).stream()
                .map(link -> link.getLearningOutcome().getId())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<LearningOutcomeRA> ras = List.of();
        Long moduleId = instrument.getActivity().getModule().getId();

        if (!targetRaIds.isEmpty()) {
            ras = learningOutcomeRARepository.findAllById(targetRaIds);

            if (ras.size() != targetRaIds.size()) {
                throw new BusinessValidationException("One or more RA ids do not exist");
            }

            for (LearningOutcomeRA ra : ras) {
                if (!ra.getModule().getId().equals(moduleId)) {
                    throw new BusinessValidationException("Instrument and RA must belong to the same module");
                }
            }

            configurationValidator.validateInstrumentRAsAllowedByUT(instrument.getActivity(), targetRaIds);
        }

        Set<Long> toRemove = new LinkedHashSet<>(currentRaIds);
        toRemove.removeAll(targetRaIds);
        for (Long raId : toRemove) {
            instrumentRARepository.deleteByInstrumentIdAndLearningOutcomeId(instrumentId, raId);
        }

        Set<Long> toAdd = new LinkedHashSet<>(targetRaIds);
        toAdd.removeAll(currentRaIds);
        for (Long raId : toAdd) {
            LearningOutcomeRA ra = ras.stream()
                    .filter(item -> item.getId().equals(raId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("RA not found: " + raId));

            instrumentRARepository.save(InstrumentRA.builder()
                    .instrument(instrument)
                    .learningOutcome(ra)
                    .build());
        }

        return InstrumentRAResponse.builder()
                .instrumentId(instrumentId)
                .utId(instrument.getActivity().getTeachingUnit().getId())
                .raIds(new ArrayList<>(targetRaIds))
                .build();
    }

    private Teacher resolveTeacher(Long teacherId, String teacherName) {
        if (teacherId != null) {
            return teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        }

        if (teacherName == null || teacherName.trim().isEmpty()) {
            throw new BusinessValidationException("teacherId or teacherName is required");
        }

        Teacher teacher = Teacher.builder()
                .fullName(teacherName.trim())
                .build();
        return teacherRepository.save(teacher);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessValidationException(message);
        }
        return value.trim();
    }

    private CourseModule getModule(Long moduleId) {
        return courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));
    }

    private LearningOutcomeRA getRA(Long raId) {
        return learningOutcomeRARepository.findById(raId)
                .orElseThrow(() -> new ResourceNotFoundException("RA not found: " + raId));
    }

    private TeachingUnitUT getUT(Long utId) {
        return teachingUnitUTRepository.findById(utId)
                .orElseThrow(() -> new ResourceNotFoundException("UT not found: " + utId));
    }

    private Activity getActivityByUT(Long utId) {
        return activityRepository.findByTeachingUnitId(utId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity for UT not found: " + utId));
    }

    private Instrument getInstrument(Long instrumentId) {
        return instrumentRepository.findDetailedById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found: " + instrumentId));
    }

    private UTRALink getUTRALinkInModule(Long moduleId, Long linkId) {
        CourseModule module = getModule(moduleId);
        UTRALink link = utraLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("UT-RA link not found: " + linkId));

        Long linkModuleId = link.getTeachingUnit().getModule().getId();
        Long raModuleId = link.getLearningOutcome().getModule().getId();
        if (!linkModuleId.equals(module.getId()) || !raModuleId.equals(module.getId())) {
            throw new BusinessValidationException("UT-RA link does not belong to module " + moduleId);
        }

        return link;
    }
}

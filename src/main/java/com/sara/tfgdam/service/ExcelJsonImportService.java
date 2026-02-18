package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.domain.entity.StudentEvaluationOverride;
import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import com.sara.tfgdam.dto.CreateInstrumentRequest;
import com.sara.tfgdam.dto.CreateRARequest;
import com.sara.tfgdam.dto.CreateStudentRequest;
import com.sara.tfgdam.dto.CreateUTRequest;
import com.sara.tfgdam.dto.ExcelImportRequest;
import com.sara.tfgdam.dto.ExcelImportResponse;
import com.sara.tfgdam.dto.GradeBatchRequest;
import com.sara.tfgdam.dto.GradeEntryRequest;
import com.sara.tfgdam.dto.SetInstrumentRAsRequest;
import com.sara.tfgdam.dto.UpsertUTRALinkRequest;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.repository.StudentEvaluationOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExcelJsonImportService {

    private final ModuleSetupService moduleSetupService;
    private final GradeService gradeService;
    private final StudentEvaluationOverrideRepository studentEvaluationOverrideRepository;

    @Transactional
    public ExcelImportResponse importExcelJson(ExcelImportRequest request) {
        validatePayloadConsistency(request);

        var module = moduleSetupService.createModule(request.getModule());

        Map<String, Long> raIdsByCode = new LinkedHashMap<>();
        Map<String, Long> raIdsByNormalizedCode = new LinkedHashMap<>();
        for (var item : request.getRas()) {
            CreateRARequest createRARequest = new CreateRARequest();
            createRARequest.setCode(item.getCode());
            createRARequest.setName(item.getName());
            createRARequest.setWeightPercent(item.getWeightPercent());

            LearningOutcomeRA ra = moduleSetupService.createRA(module.getId(), createRARequest);
            raIdsByCode.put(item.getCode().trim(), ra.getId());
            raIdsByNormalizedCode.put(normalizeKey(item.getCode()), ra.getId());
        }

        Map<String, Long> utIdsByKey = new LinkedHashMap<>();
        for (var utItem : request.getUts()) {
            CreateUTRequest createUTRequest = new CreateUTRequest();
            createUTRequest.setName(utItem.getName());
            createUTRequest.setEvaluationPeriod(utItem.getEvaluationPeriod());

            TeachingUnitUT ut = moduleSetupService.createUT(module.getId(), createUTRequest);
            utIdsByKey.put(utItem.getKey().trim(), ut.getId());
        }

        Map<String, Long> utIdsByNormalizedKey = new LinkedHashMap<>();
        for (var entry : utIdsByKey.entrySet()) {
            utIdsByNormalizedKey.put(normalizeKey(entry.getKey()), entry.getValue());
        }

        Map<String, Long> instrumentIdsByKey = new LinkedHashMap<>();
        Map<String, Long> instrumentIdsByNormalizedKey = new LinkedHashMap<>();

        for (var utItem : request.getUts()) {
            Long utId = utIdsByNormalizedKey.get(normalizeKey(utItem.getKey()));
            if (utId == null) {
                throw new BusinessValidationException("UT key not found during import: " + utItem.getKey());
            }

            for (var distribution : utItem.getRaDistributions()) {
                Long raId = raIdsByNormalizedCode.get(normalizeKey(distribution.getRaCode()));
                if (raId == null) {
                    throw new BusinessValidationException("RA code not found for UT-RA distribution: " + distribution.getRaCode());
                }

                UpsertUTRALinkRequest upsert = new UpsertUTRALinkRequest();
                upsert.setUtId(utId);
                upsert.setRaId(raId);
                upsert.setPercent(distribution.getPercent());
                moduleSetupService.upsertUTRALink(module.getId(), upsert);
            }

            for (var instrumentItem : utItem.getInstruments()) {
                CreateInstrumentRequest createInstrumentRequest = new CreateInstrumentRequest();
                createInstrumentRequest.setName(instrumentItem.getName());
                createInstrumentRequest.setWeightPercent(instrumentItem.getWeightPercent());

                Instrument instrument = moduleSetupService.createInstrument(utId, createInstrumentRequest);

                List<Long> raIds = instrumentItem.getRaCodes().stream()
                        .map(this::normalizeKey)
                        .map(code -> {
                            Long raId = raIdsByNormalizedCode.get(code);
                            if (raId == null) {
                                throw new BusinessValidationException("RA code not found for instrument: " + code);
                            }
                            return raId;
                        })
                        .toList();

                SetInstrumentRAsRequest setRequest = new SetInstrumentRAsRequest();
                setRequest.setRaIds(raIds);
                moduleSetupService.setInstrumentRAs(instrument.getId(), setRequest);

                String originalKey = instrumentItem.getKey().trim();
                String normalizedKey = normalizeKey(originalKey);
                instrumentIdsByKey.put(originalKey, instrument.getId());
                instrumentIdsByNormalizedKey.put(normalizedKey, instrument.getId());
            }
        }

        List<ExcelImportRequest.StudentItem> students = request.getStudents() == null ? List.of() : request.getStudents();
        Map<String, Long> studentIdsByCode = new LinkedHashMap<>();
        Map<String, Student> studentsByNormalizedCode = new LinkedHashMap<>();
        List<GradeEntryRequest> gradeEntries = new ArrayList<>();

        for (var studentItem : students) {
            CreateStudentRequest createStudentRequest = new CreateStudentRequest();
            createStudentRequest.setModuleId(module.getId());
            createStudentRequest.setStudentCode(studentItem.getStudentCode());
            createStudentRequest.setFullName(studentItem.getFullName());

            Student student = moduleSetupService.createStudent(createStudentRequest);
            studentIdsByCode.put(studentItem.getStudentCode().trim(), student.getId());
            studentsByNormalizedCode.put(normalizeKey(studentItem.getStudentCode()), student);

            List<ExcelImportRequest.GradeItem> grades = studentItem.getGrades() == null ? List.of() : studentItem.getGrades();
            for (var gradeItem : grades) {
                Long instrumentId = instrumentIdsByNormalizedKey.get(normalizeKey(gradeItem.getInstrumentKey()));
                if (instrumentId == null) {
                    throw new BusinessValidationException("instrumentKey not found in payload: " + gradeItem.getInstrumentKey());
                }

                GradeEntryRequest gradeEntry = new GradeEntryRequest();
                gradeEntry.setStudentId(student.getId());
                gradeEntry.setInstrumentId(instrumentId);
                gradeEntry.setGradeValue(gradeItem.getGradeValue());
                gradeEntries.add(gradeEntry);
            }
        }

        int gradeCount = 0;
        if (!gradeEntries.isEmpty()) {
            GradeBatchRequest batchRequest = new GradeBatchRequest();
            batchRequest.setGrades(gradeEntries);
            gradeCount = gradeService.upsertGrades(batchRequest).size();
        }

        List<ExcelImportRequest.EvaluationOverrideItem> evaluationOverrides =
                request.getEvaluationOverrides() == null ? List.of() : request.getEvaluationOverrides();
        if (!evaluationOverrides.isEmpty()) {
            List<StudentEvaluationOverride> overrides = new ArrayList<>();
            for (ExcelImportRequest.EvaluationOverrideItem item : evaluationOverrides) {
                Student student = studentsByNormalizedCode.get(normalizeKey(item.getStudentCode()));
                if (student == null) {
                    throw new BusinessValidationException("evaluationOverride references unknown studentCode: " + item.getStudentCode());
                }

                overrides.add(StudentEvaluationOverride.builder()
                        .student(student)
                        .evaluationPeriod(item.getEvaluationPeriod())
                        .numericGrade(item.getNumericGrade())
                        .suggestedBulletinGrade(item.getSuggestedBulletinGrade())
                        .allRAsPassed(item.getAllRAsPassed())
                        .build());
            }
            studentEvaluationOverrideRepository.saveAll(overrides);
        }

        return ExcelImportResponse.builder()
                .moduleId(module.getId())
                .raCount(raIdsByCode.size())
                .utCount(utIdsByKey.size())
                .instrumentCount(instrumentIdsByKey.size())
                .studentCount(studentIdsByCode.size())
                .gradeCount(gradeCount)
                .raIdsByCode(raIdsByCode)
                .utIdsByKey(utIdsByKey)
                .instrumentIdsByKey(instrumentIdsByKey)
                .studentIdsByCode(studentIdsByCode)
                .build();
    }

    private void validatePayloadConsistency(ExcelImportRequest request) {
        Set<String> normalizedRaCodes = new LinkedHashSet<>();
        for (var ra : request.getRas()) {
            String code = normalizeKey(ra.getCode());
            if (!normalizedRaCodes.add(code)) {
                throw new BusinessValidationException("Duplicated RA code in payload: " + ra.getCode().trim());
            }
        }

        Set<String> normalizedUtKeys = new LinkedHashSet<>();
        Set<Integer> evaluationPeriods = new LinkedHashSet<>();
        Set<String> normalizedInstrumentKeys = new LinkedHashSet<>();

        for (var ut : request.getUts()) {
            String utKey = normalizeKey(ut.getKey());
            if (!normalizedUtKeys.add(utKey)) {
                throw new BusinessValidationException("Duplicated UT key in payload: " + ut.getKey().trim());
            }
            evaluationPeriods.add(ut.getEvaluationPeriod());

            for (var distribution : ut.getRaDistributions()) {
                String raCode = normalizeKey(distribution.getRaCode());
                if (!normalizedRaCodes.contains(raCode)) {
                    throw new BusinessValidationException(
                            "UT-RA distribution references unknown RA code: " + distribution.getRaCode().trim()
                    );
                }
            }

            for (var instrument : ut.getInstruments()) {
                String instrumentKey = normalizeKey(instrument.getKey());
                if (!normalizedInstrumentKeys.add(instrumentKey)) {
                    throw new BusinessValidationException("Duplicated instrument key in payload: " + instrument.getKey().trim());
                }

                for (String raCodeRaw : instrument.getRaCodes()) {
                    String raCode = normalizeKey(raCodeRaw);
                    if (!normalizedRaCodes.contains(raCode)) {
                        throw new BusinessValidationException(
                                "Instrument references unknown RA code: " + raCodeRaw.trim()
                        );
                    }
                }
            }
        }

        List<ExcelImportRequest.StudentItem> students = request.getStudents() == null ? List.of() : request.getStudents();
        Set<String> normalizedStudentCodes = new LinkedHashSet<>();
        for (var student : students) {
            String studentCode = normalizeKey(student.getStudentCode());
            if (!normalizedStudentCodes.add(studentCode)) {
                throw new BusinessValidationException("Duplicated studentCode in payload: " + student.getStudentCode().trim());
            }

            List<ExcelImportRequest.GradeItem> grades = student.getGrades() == null ? List.of() : student.getGrades();
            for (var grade : grades) {
                String instrumentKey = normalizeKey(grade.getInstrumentKey());
                if (!normalizedInstrumentKeys.contains(instrumentKey)) {
                    throw new BusinessValidationException(
                            "Student grade references unknown instrumentKey: " + grade.getInstrumentKey().trim()
                    );
                }
            }
        }

        List<ExcelImportRequest.EvaluationOverrideItem> evaluationOverrides =
                request.getEvaluationOverrides() == null ? List.of() : request.getEvaluationOverrides();
        Set<String> overrideKeys = new LinkedHashSet<>();
        for (ExcelImportRequest.EvaluationOverrideItem override : evaluationOverrides) {
            String studentCode = normalizeKey(override.getStudentCode());
            if (!normalizedStudentCodes.contains(studentCode)) {
                throw new BusinessValidationException(
                        "evaluationOverride references unknown studentCode: " + override.getStudentCode().trim()
                );
            }

            if (!evaluationPeriods.contains(override.getEvaluationPeriod())) {
                throw new BusinessValidationException(
                        "evaluationOverride references unknown evaluationPeriod: " + override.getEvaluationPeriod()
                );
            }

            String key = studentCode + "#" + override.getEvaluationPeriod();
            if (!overrideKeys.add(key)) {
                throw new BusinessValidationException(
                        "Duplicated evaluationOverride for studentCode/evaluationPeriod: "
                                + override.getStudentCode().trim() + "/" + override.getEvaluationPeriod()
                );
            }
        }
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

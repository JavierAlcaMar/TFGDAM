package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.dto.GradeBatchRequest;
import com.sara.tfgdam.dto.GradeEntryRequest;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.validation.ConfigurationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final ConfigurationValidator configurationValidator;

    @Transactional
    public List<Grade> upsertGrades(GradeBatchRequest request) {
        Set<Long> moduleIdsToValidate = new HashSet<>();
        List<Grade> results = new ArrayList<>();

        for (GradeEntryRequest entry : request.getGrades()) {
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

            Grade grade = gradeRepository.findByStudentIdAndInstrumentId(student.getId(), instrument.getId())
                    .orElseGet(() -> Grade.builder()
                            .student(student)
                            .instrument(instrument)
                            .build());

            grade.setGradeValue(entry.getGradeValue());
            results.add(gradeRepository.save(grade));
            moduleIdsToValidate.add(studentModuleId);
        }

        for (Long moduleId : moduleIdsToValidate) {
            configurationValidator.validateModuleReadyForCalculations(moduleId);
        }

        return results;
    }
}

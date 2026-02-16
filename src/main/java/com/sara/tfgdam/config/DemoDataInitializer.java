package com.sara.tfgdam.config;

import com.sara.tfgdam.domain.entity.Activity;
import com.sara.tfgdam.domain.entity.CourseModule;
import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.InstrumentRA;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.domain.entity.Teacher;
import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import com.sara.tfgdam.domain.entity.UTRALink;
import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.GradeRepository;
import com.sara.tfgdam.repository.InstrumentRARepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.StudentRepository;
import com.sara.tfgdam.repository.TeacherRepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.repository.UTRALinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DemoDataInitializer {

    private final TeacherRepository teacherRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;
    private final UTRALinkRepository utraLinkRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;
    private final InstrumentRARepository instrumentRARepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {
            if (courseModuleRepository.count() > 0) {
                return;
            }

            Teacher teacher = teacherRepository.save(Teacher.builder().fullName("Docente Demo").build());
            CourseModule module = courseModuleRepository.save(CourseModule.builder()
                    .name("Sistemas Informaticos")
                    .academicYear("2025-2026")
                    .teacher(teacher)
                    .build());

            LearningOutcomeRA ra1 = learningOutcomeRARepository.save(LearningOutcomeRA.builder()
                    .module(module)
                    .code("RA1")
                    .name("Configura sistemas")
                    .weightPercent(new BigDecimal("60.00"))
                    .build());
            LearningOutcomeRA ra2 = learningOutcomeRARepository.save(LearningOutcomeRA.builder()
                    .module(module)
                    .code("RA2")
                    .name("Administra sistemas")
                    .weightPercent(new BigDecimal("40.00"))
                    .build());

            TeachingUnitUT ut1 = teachingUnitUTRepository.save(TeachingUnitUT.builder()
                    .module(module)
                    .name("UT1 Fundamentos")
                    .evaluationPeriod(1)
                    .build());
            TeachingUnitUT ut2 = teachingUnitUTRepository.save(TeachingUnitUT.builder()
                    .module(module)
                    .name("UT2 Servicios")
                    .evaluationPeriod(2)
                    .build());

            Activity act1 = activityRepository.save(Activity.builder()
                    .module(module)
                    .teachingUnit(ut1)
                    .name("UT1 Fundamentos")
                    .build());
            Activity act2 = activityRepository.save(Activity.builder()
                    .module(module)
                    .teachingUnit(ut2)
                    .name("UT2 Servicios")
                    .build());

            utraLinkRepository.save(UTRALink.builder().teachingUnit(ut1).learningOutcome(ra1).percent(new BigDecimal("50.00")).build());
            utraLinkRepository.save(UTRALink.builder().teachingUnit(ut2).learningOutcome(ra1).percent(new BigDecimal("50.00")).build());
            utraLinkRepository.save(UTRALink.builder().teachingUnit(ut1).learningOutcome(ra2).percent(new BigDecimal("40.00")).build());
            utraLinkRepository.save(UTRALink.builder().teachingUnit(ut2).learningOutcome(ra2).percent(new BigDecimal("60.00")).build());

            Instrument i11 = instrumentRepository.save(Instrument.builder()
                    .activity(act1)
                    .name("Examen UT1")
                    .weightPercent(new BigDecimal("60.00"))
                    .build());
            Instrument i12 = instrumentRepository.save(Instrument.builder()
                    .activity(act1)
                    .name("Practica UT1")
                    .weightPercent(new BigDecimal("40.00"))
                    .build());

            Instrument i21 = instrumentRepository.save(Instrument.builder()
                    .activity(act2)
                    .name("Examen UT2")
                    .weightPercent(new BigDecimal("70.00"))
                    .build());
            Instrument i22 = instrumentRepository.save(Instrument.builder()
                    .activity(act2)
                    .name("Proyecto UT2")
                    .weightPercent(new BigDecimal("30.00"))
                    .build());

            instrumentRARepository.save(InstrumentRA.builder().instrument(i11).learningOutcome(ra1).build());
            instrumentRARepository.save(InstrumentRA.builder().instrument(i11).learningOutcome(ra2).build());
            instrumentRARepository.save(InstrumentRA.builder().instrument(i12).learningOutcome(ra1).build());
            instrumentRARepository.save(InstrumentRA.builder().instrument(i21).learningOutcome(ra1).build());
            instrumentRARepository.save(InstrumentRA.builder().instrument(i22).learningOutcome(ra2).build());

            Student s1 = studentRepository.save(Student.builder()
                    .module(module)
                    .studentCode("A001")
                    .fullName("Ana Perez")
                    .build());
            Student s2 = studentRepository.save(Student.builder()
                    .module(module)
                    .studentCode("A002")
                    .fullName("Luis Garcia")
                    .build());

            gradeRepository.save(Grade.builder().student(s1).instrument(i11).gradeValue(new BigDecimal("7.50")).build());
            gradeRepository.save(Grade.builder().student(s1).instrument(i12).gradeValue(new BigDecimal("8.00")).build());
            gradeRepository.save(Grade.builder().student(s1).instrument(i21).gradeValue(new BigDecimal("6.75")).build());
            gradeRepository.save(Grade.builder().student(s1).instrument(i22).gradeValue(new BigDecimal("7.25")).build());

            gradeRepository.save(Grade.builder().student(s2).instrument(i11).gradeValue(new BigDecimal("4.00")).build());
            gradeRepository.save(Grade.builder().student(s2).instrument(i12).gradeValue(new BigDecimal("5.00")).build());
            gradeRepository.save(Grade.builder().student(s2).instrument(i21).gradeValue(new BigDecimal("4.50")).build());
            gradeRepository.save(Grade.builder().student(s2).instrument(i22).gradeValue(new BigDecimal("5.50")).build());
        };
    }
}

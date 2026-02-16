package com.sara.tfgdam.mapper;

import com.sara.tfgdam.domain.entity.CourseModule;
import com.sara.tfgdam.domain.entity.Grade;
import com.sara.tfgdam.domain.entity.ImportJob;
import com.sara.tfgdam.domain.entity.Instrument;
import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import com.sara.tfgdam.domain.entity.Student;
import com.sara.tfgdam.domain.entity.Teacher;
import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import com.sara.tfgdam.domain.entity.UTRALink;
import com.sara.tfgdam.domain.entity.UserAccount;
import com.sara.tfgdam.dto.GradeResponse;
import com.sara.tfgdam.dto.ImportJobResponse;
import com.sara.tfgdam.dto.InstrumentResponse;
import com.sara.tfgdam.dto.ModuleResponse;
import com.sara.tfgdam.dto.RAResponse;
import com.sara.tfgdam.dto.StudentResponse;
import com.sara.tfgdam.dto.TeacherResponse;
import com.sara.tfgdam.dto.UTRALinkResponse;
import com.sara.tfgdam.dto.UTResponse;
import com.sara.tfgdam.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public TeacherResponse toTeacherResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .fullName(teacher.getFullName())
                .build();
    }

    public ModuleResponse toModuleResponse(CourseModule module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .name(module.getName())
                .academicYear(module.getAcademicYear())
                .teacherId(module.getTeacher() != null ? module.getTeacher().getId() : null)
                .teacherName(module.getTeacher() != null ? module.getTeacher().getFullName() : null)
                .build();
    }

    public RAResponse toRAResponse(LearningOutcomeRA ra) {
        return RAResponse.builder()
                .id(ra.getId())
                .moduleId(ra.getModule().getId())
                .code(ra.getCode())
                .name(ra.getName())
                .weightPercent(ra.getWeightPercent())
                .build();
    }

    public UTResponse toUTResponse(TeachingUnitUT ut, Long activityId) {
        return UTResponse.builder()
                .id(ut.getId())
                .moduleId(ut.getModule().getId())
                .activityId(activityId)
                .name(ut.getName())
                .evaluationPeriod(ut.getEvaluationPeriod())
                .build();
    }

    public UTRALinkResponse toUTRALinkResponse(UTRALink link) {
        return UTRALinkResponse.builder()
                .id(link.getId())
                .utId(link.getTeachingUnit().getId())
                .raId(link.getLearningOutcome().getId())
                .percent(link.getPercent())
                .build();
    }

    public InstrumentResponse toInstrumentResponse(Instrument instrument) {
        return InstrumentResponse.builder()
                .id(instrument.getId())
                .activityId(instrument.getActivity().getId())
                .utId(instrument.getActivity().getTeachingUnit().getId())
                .name(instrument.getName())
                .weightPercent(instrument.getWeightPercent())
                .build();
    }

    public StudentResponse toStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .moduleId(student.getModule().getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .build();
    }

    public GradeResponse toGradeResponse(Grade grade) {
        return GradeResponse.builder()
                .id(grade.getId())
                .studentId(grade.getStudent().getId())
                .instrumentId(grade.getInstrument().getId())
                .gradeValue(grade.getGradeValue())
                .build();
    }

    public ImportJobResponse toImportJobResponse(ImportJob job) {
        return ImportJobResponse.builder()
                .id(job.getId())
                .moduleId(job.getModuleId())
                .filename(job.getFilename())
                .status(job.getStatus())
                .resultJson(job.getResultJson())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public UserResponse toUserResponse(UserAccount user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

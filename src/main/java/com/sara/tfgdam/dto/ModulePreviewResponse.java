package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class ModulePreviewResponse {
    Long moduleId;
    String moduleName;
    String academicYear;
    String teacherName;
    List<RAItem> ras;
    List<UTItem> uts;
    List<UTRALinkItem> utRaLinks;
    List<ActivityItem> activities;
    List<InstrumentItem> instruments;
    List<StudentItem> students;
    List<GradeItem> grades;

    @Value
    @Builder
    public static class RAItem {
        Long id;
        String code;
        String name;
        BigDecimal weightPercent;
    }

    @Value
    @Builder
    public static class UTItem {
        Long id;
        String name;
        Integer evaluationPeriod;
    }

    @Value
    @Builder
    public static class UTRALinkItem {
        Long id;
        Long utId;
        Long raId;
        BigDecimal percent;
    }

    @Value
    @Builder
    public static class ActivityItem {
        Long id;
        Long utId;
        Integer evaluationPeriod;
        String name;
    }

    @Value
    @Builder
    public static class InstrumentItem {
        Long id;
        Long activityId;
        Long utId;
        String name;
        BigDecimal weightPercent;
        List<Long> raIds;
    }

    @Value
    @Builder
    public static class StudentItem {
        Long id;
        String studentCode;
        String fullName;
    }

    @Value
    @Builder
    public static class GradeItem {
        Long id;
        Long studentId;
        Long instrumentId;
        BigDecimal gradeValue;
    }
}

package com.sara.tfgdam.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "student_evaluation_overrides",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_eval_override_student_period", columnNames = {"student_id", "evaluation_period"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEvaluationOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "evaluation_period", nullable = false)
    private Integer evaluationPeriod;

    @Column(name = "numeric_grade", nullable = false, precision = 6, scale = 4)
    private BigDecimal numericGrade;

    @Column(name = "suggested_bulletin_grade", nullable = false)
    private Integer suggestedBulletinGrade;

    @Column(name = "all_ras_passed", nullable = false)
    private Boolean allRAsPassed;
}

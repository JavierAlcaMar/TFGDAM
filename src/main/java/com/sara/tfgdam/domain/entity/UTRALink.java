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
        name = "ut_ra_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ut_ra", columnNames = {"ut_id", "ra_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UTRALink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ut_id", nullable = false)
    private TeachingUnitUT teachingUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ra_id", nullable = false)
    private LearningOutcomeRA learningOutcome;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;
}

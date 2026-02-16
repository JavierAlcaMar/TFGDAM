package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.TeachingUnitUT;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeachingUnitUTRepository extends JpaRepository<TeachingUnitUT, Long> {

    List<TeachingUnitUT> findByModuleId(Long moduleId);

    List<TeachingUnitUT> findByModuleIdAndEvaluationPeriod(Long moduleId, Integer evaluationPeriod);
}

package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.StudentEvaluationOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEvaluationOverrideRepository extends JpaRepository<StudentEvaluationOverride, Long> {

    List<StudentEvaluationOverride> findByStudent_Module_IdAndEvaluationPeriod(Long moduleId, Integer evaluationPeriod);

    void deleteByStudent_Module_Id(Long moduleId);
}

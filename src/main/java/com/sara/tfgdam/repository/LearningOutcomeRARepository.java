package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.LearningOutcomeRA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningOutcomeRARepository extends JpaRepository<LearningOutcomeRA, Long> {

    List<LearningOutcomeRA> findByModuleId(Long moduleId);
}

package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.UTRALink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UTRALinkRepository extends JpaRepository<UTRALink, Long> {

    List<UTRALink> findByLearningOutcomeId(Long learningOutcomeId);

    List<UTRALink> findByTeachingUnitId(Long teachingUnitId);

    List<UTRALink> findByTeachingUnitIdIn(List<Long> teachingUnitIds);

    Optional<UTRALink> findByTeachingUnitIdAndLearningOutcomeId(Long teachingUnitId, Long learningOutcomeId);

    void deleteByLearningOutcomeId(Long learningOutcomeId);

    void deleteByTeachingUnitId(Long teachingUnitId);
}

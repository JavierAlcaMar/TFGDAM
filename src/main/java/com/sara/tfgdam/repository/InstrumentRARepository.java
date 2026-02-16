package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.InstrumentRA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentRARepository extends JpaRepository<InstrumentRA, Long> {

    List<InstrumentRA> findByInstrumentId(Long instrumentId);

    List<InstrumentRA> findByInstrumentIdIn(List<Long> instrumentIds);

    Optional<InstrumentRA> findByInstrumentIdAndLearningOutcomeId(Long instrumentId, Long learningOutcomeId);

    void deleteByInstrumentId(Long instrumentId);

    void deleteByInstrumentIdIn(List<Long> instrumentIds);

    void deleteByInstrumentIdAndLearningOutcomeId(Long instrumentId, Long learningOutcomeId);

    void deleteByLearningOutcomeId(Long learningOutcomeId);
}

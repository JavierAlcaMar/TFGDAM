package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.InstrumentExerciseWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentExerciseWeightRepository extends JpaRepository<InstrumentExerciseWeight, Long> {

    List<InstrumentExerciseWeight> findByInstrumentId(Long instrumentId);

    List<InstrumentExerciseWeight> findByInstrumentIdIn(List<Long> instrumentIds);

    void deleteByInstrumentId(Long instrumentId);

    void deleteByInstrumentIdIn(List<Long> instrumentIds);
}


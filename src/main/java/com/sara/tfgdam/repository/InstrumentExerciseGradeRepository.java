package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.InstrumentExerciseGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentExerciseGradeRepository extends JpaRepository<InstrumentExerciseGrade, Long> {

    List<InstrumentExerciseGrade> findByStudent_IdIn(List<Long> studentIds);

    List<InstrumentExerciseGrade> findByStudent_IdAndInstrument_Id(Long studentId, Long instrumentId);

    void deleteByStudent_IdAndInstrument_Id(Long studentId, Long instrumentId);

    void deleteByInstrument_Id(Long instrumentId);

    void deleteByInstrument_IdIn(List<Long> instrumentIds);

    void deleteByStudent_IdIn(List<Long> studentIds);
}

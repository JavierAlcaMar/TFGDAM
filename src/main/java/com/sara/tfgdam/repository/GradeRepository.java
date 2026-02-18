package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByStudentIdIn(List<Long> studentIds);

    Optional<Grade> findByStudentIdAndInstrumentId(Long studentId, Long instrumentId);

    void deleteByInstrumentId(Long instrumentId);

    void deleteByInstrumentIdIn(List<Long> instrumentIds);

    void deleteByStudentIdIn(List<Long> studentIds);
}

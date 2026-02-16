package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    List<Instrument> findByActivityId(Long activityId);

    List<Instrument> findByActivityIdIn(List<Long> activityIds);

    @Query("""
            select i
            from Instrument i
            join fetch i.activity a
            join fetch a.teachingUnit
            where i.id = :id
            """)
    Optional<Instrument> findDetailedById(@Param("id") Long id);
}

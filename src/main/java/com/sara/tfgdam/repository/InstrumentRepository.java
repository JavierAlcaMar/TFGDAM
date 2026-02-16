package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    List<Instrument> findByActivityId(Long activityId);

    List<Instrument> findByActivityIdIn(List<Long> activityIds);
}

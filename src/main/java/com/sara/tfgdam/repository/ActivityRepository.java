package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Optional<Activity> findByTeachingUnitId(Long teachingUnitId);

    List<Activity> findByModuleId(Long moduleId);
}

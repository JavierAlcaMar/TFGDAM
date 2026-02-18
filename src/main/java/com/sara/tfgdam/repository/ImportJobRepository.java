package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    void deleteByModuleId(Long moduleId);
}

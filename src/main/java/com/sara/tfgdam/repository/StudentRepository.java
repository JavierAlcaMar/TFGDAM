package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByModuleId(Long moduleId);

    Optional<Student> findByModuleIdAndStudentCode(Long moduleId, String studentCode);
}

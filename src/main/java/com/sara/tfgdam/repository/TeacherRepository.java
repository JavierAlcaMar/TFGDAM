package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}

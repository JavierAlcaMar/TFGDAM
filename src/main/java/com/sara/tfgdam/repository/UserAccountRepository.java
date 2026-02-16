package com.sara.tfgdam.repository;

import com.sara.tfgdam.domain.entity.UserAccount;
import com.sara.tfgdam.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select case when count(u) > 0 then true else false end from UserAccount u join u.roles r where r = :role")
    boolean existsByRole(@Param("role") UserRole role);

    List<UserAccount> findAllByOrderByEmailAsc();
}

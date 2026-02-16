package com.sara.tfgdam.config;

import com.sara.tfgdam.domain.entity.UserAccount;
import com.sara.tfgdam.domain.entity.UserRole;
import com.sara.tfgdam.domain.entity.UserStatus;
import com.sara.tfgdam.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SuperAdminBootstrap {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedSuperAdmin() {
        return args -> {
            if (userAccountRepository.existsByRole(UserRole.ROLE_SUPERADMIN)) {
                return;
            }

            if (userAccountRepository.existsByEmailIgnoreCase("admin@admin.com")) {
                return;
            }

            UserAccount admin = UserAccount.builder()
                    .email("admin@admin.com")
                    .passwordHash(passwordEncoder.encode("adminmiralmonte"))
                    .roles(Set.of(UserRole.ROLE_SUPERADMIN))
                    .status(UserStatus.ACTIVE)
                    .build();

            userAccountRepository.save(admin);
        };
    }
}

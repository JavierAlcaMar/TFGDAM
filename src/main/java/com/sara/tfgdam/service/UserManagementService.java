package com.sara.tfgdam.service;

import com.sara.tfgdam.domain.entity.UserAccount;
import com.sara.tfgdam.domain.entity.UserRole;
import com.sara.tfgdam.domain.entity.UserStatus;
import com.sara.tfgdam.dto.CreateUserRequest;
import com.sara.tfgdam.dto.PatchUserRolesRequest;
import com.sara.tfgdam.exception.BusinessValidationException;
import com.sara.tfgdam.exception.ResourceNotFoundException;
import com.sara.tfgdam.repository.UserAccountRepository;
import com.sara.tfgdam.security.AuthenticatedUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String SCHOOL_DOMAIN = "@colegiomiralmonte.es";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public UserAccount createUser(CreateUserRequest request) {
        UserAccount actor = authenticatedUserResolver.getCurrentUser();
        String email = normalizeEmail(request.getEmail());

        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessValidationException("User with email already exists: " + email);
        }

        Set<UserRole> roles = resolveCreationRoles(actor, request.getRoles());
        UserStatus status = request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus();

        validateDomainByRoles(email, roles);

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .status(status)
                .build();

        return userAccountRepository.save(user);
    }

    @Transactional
    public UserAccount activateUser(Long userId) {
        UserAccount actor = authenticatedUserResolver.getCurrentUser();
        UserAccount target = getUser(userId);

        validateActorCanManageTarget(actor, target);
        target.setStatus(UserStatus.ACTIVE);
        return userAccountRepository.save(target);
    }

    @Transactional
    public UserAccount disableUser(Long userId) {
        UserAccount actor = authenticatedUserResolver.getCurrentUser();
        UserAccount target = getUser(userId);

        validateActorCanManageTarget(actor, target);
        target.setStatus(UserStatus.DISABLED);
        return userAccountRepository.save(target);
    }

    @Transactional
    public UserAccount patchRoles(Long userId, PatchUserRolesRequest request) {
        UserAccount actor = authenticatedUserResolver.getCurrentUser();
        if (!isSuperAdmin(actor)) {
            throw new BusinessValidationException("Only SUPERADMIN can assign roles");
        }

        UserAccount target = getUser(userId);
        Set<UserRole> roles = normalizeRequestedRoles(request.getRoles());
        validateDomainByRoles(target.getEmail(), roles);

        target.setRoles(roles);
        return userAccountRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listUsers() {
        UserAccount actor = authenticatedUserResolver.getCurrentUser();

        if (isSuperAdmin(actor)) {
            return userAccountRepository.findAllByOrderByEmailAsc();
        }

        if (isDirector(actor)) {
            return userAccountRepository.findAllByOrderByEmailAsc().stream()
                    .filter(user -> user.getId().equals(actor.getId()) || isStrictTeacher(user))
                    .toList();
        }

        throw new BusinessValidationException("Only SUPERADMIN or DIRECTOR can list users");
    }

    @Transactional(readOnly = true)
    public UserAccount getCurrentUser() {
        return authenticatedUserResolver.getCurrentUser();
    }

    private Set<UserRole> resolveCreationRoles(UserAccount actor, Set<UserRole> requestedRoles) {
        if (isSuperAdmin(actor)) {
            if (requestedRoles == null || requestedRoles.isEmpty()) {
                throw new BusinessValidationException("roles are required");
            }
            return normalizeRequestedRoles(requestedRoles);
        }

        if (isDirector(actor)) {
            if (requestedRoles != null && !requestedRoles.isEmpty()) {
                Set<UserRole> normalized = normalizeRequestedRoles(requestedRoles);
                if (!(normalized.size() == 1 && normalized.contains(UserRole.ROLE_TEACHER))) {
                    throw new BusinessValidationException("DIRECTOR can only create TEACHER users");
                }
            }
            return Set.of(UserRole.ROLE_TEACHER);
        }

        throw new BusinessValidationException("Current user cannot create users");
    }

    private Set<UserRole> normalizeRequestedRoles(Set<UserRole> roles) {
        Set<UserRole> normalized = new HashSet<>(roles);
        if (normalized.contains(UserRole.ROLE_DIRECTOR)) {
            normalized.add(UserRole.ROLE_TEACHER);
        }
        return normalized;
    }

    private void validateDomainByRoles(String email, Set<UserRole> roles) {
        boolean requiresSchoolDomain = roles.contains(UserRole.ROLE_DIRECTOR) || roles.contains(UserRole.ROLE_TEACHER);
        if (requiresSchoolDomain && !email.toLowerCase().endsWith(SCHOOL_DOMAIN)) {
            throw new BusinessValidationException("Email must end with " + SCHOOL_DOMAIN + " for DIRECTOR/TEACHER roles");
        }
    }

    private void validateActorCanManageTarget(UserAccount actor, UserAccount target) {
        if (isSuperAdmin(actor)) {
            return;
        }

        if (isDirector(actor)) {
            if (!isStrictTeacher(target)) {
                throw new BusinessValidationException("DIRECTOR can only manage TEACHER users");
            }
            return;
        }

        throw new BusinessValidationException("Current user cannot manage users");
    }

    private boolean isSuperAdmin(UserAccount user) {
        return user.getRoles().contains(UserRole.ROLE_SUPERADMIN);
    }

    private boolean isDirector(UserAccount user) {
        return user.getRoles().contains(UserRole.ROLE_DIRECTOR);
    }

    private boolean isStrictTeacher(UserAccount user) {
        return user.getRoles().contains(UserRole.ROLE_TEACHER)
                && !user.getRoles().contains(UserRole.ROLE_DIRECTOR)
                && !user.getRoles().contains(UserRole.ROLE_SUPERADMIN);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BusinessValidationException("email is required");
        }
        String value = email.trim().toLowerCase();
        if (value.isEmpty()) {
            throw new BusinessValidationException("email is required");
        }
        return value;
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}

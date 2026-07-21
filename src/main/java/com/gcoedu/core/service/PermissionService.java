package com.gcoedu.core.service;

import com.gcoedu.core.domain.entity.publics.Manager;
import com.gcoedu.core.domain.entity.publics.RoleEnum;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.School;
import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import com.gcoedu.core.domain.entity.tenant.Student;
import com.gcoedu.core.repository.publics.ManagerRepository;
import com.gcoedu.core.repository.publics.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;

    public PermissionService(UserRepository userRepository, ManagerRepository managerRepository) {
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByEmail(username)
                .orElseGet(() -> userRepository.findByRegistration(username).orElse(null));
    }

    public boolean canAccessSchool(String schoolId) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }

        // Master Admin (ADMIN) has access to all schools in the active tenant/city schema
        if (user.getRole() == RoleEnum.ADMIN) {
            return true;
        }

        // Municipality Admin (TECADM) has access to all schools in their active tenant/city schema
        if (user.getRole() == RoleEnum.TECADM) {
            return true;
        }

        // School Director / Coordinator can only access their linked school
        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            Optional<Manager> managerOpt = managerRepository.findByUserId(user.getId());
            if (managerOpt.isPresent()) {
                String managerSchoolId = managerOpt.get().getSchoolId();
                return schoolId != null && schoolId.equals(managerSchoolId);
            }
            return false;
        }

        // Default to true for other roles (like teachers/students) unless specific school level filtering is requested
        return true;
    }

    public boolean canAccessClass(SchoolClass schoolClass) {
        if (schoolClass == null) {
            return false;
        }
        String schoolId = schoolClass.getSchool() != null ? schoolClass.getSchool().getId() : null;
        return canAccessSchool(schoolId);
    }

    public boolean canAccessStudent(Student student) {
        if (student == null) {
            return false;
        }
        String schoolId = student.getSchool() != null ? student.getSchool().getId() : null;
        return canAccessSchool(schoolId);
    }

    public List<School> filterSchools(List<School> schools) {
        User user = getCurrentUser();
        if (user == null) {
            return Collections.emptyList();
        }

        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.TECADM) {
            return schools;
        }

        if (user.getRole() == RoleEnum.DIRETOR || user.getRole() == RoleEnum.COORDENADOR) {
            Optional<Manager> managerOpt = managerRepository.findByUserId(user.getId());
            if (managerOpt.isPresent()) {
                String schoolId = managerOpt.get().getSchoolId();
                return schools.stream()
                        .filter(s -> s.getId().equals(schoolId))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
        return schools;
    }

    public String getCurrentUserCityId() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getCity() != null ? user.getCity().getId() : null;
    }

    public String getCurrentManagedSchoolId() {
        User user = getCurrentUser();
        if (user == null
                || (user.getRole() != RoleEnum.DIRETOR && user.getRole() != RoleEnum.COORDENADOR)) {
            return null;
        }
        return managerRepository.findByUserId(user.getId())
                .map(Manager::getSchoolId)
                .orElse(null);
    }
}

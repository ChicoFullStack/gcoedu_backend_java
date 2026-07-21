package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    boolean existsByRegistration(String registration);
    java.util.List<Student> findBySchoolClassId(String schoolClassId);
    Optional<Student> findByUserId(String userId);
    long countBySchoolId(String schoolId);
    long countBySchoolClassId(String schoolClassId);
}

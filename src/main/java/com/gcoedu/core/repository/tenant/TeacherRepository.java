package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {
    Optional<Teacher> findByUserId(String userId);
}

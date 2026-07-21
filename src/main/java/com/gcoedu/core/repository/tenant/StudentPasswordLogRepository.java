package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.StudentPasswordLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentPasswordLogRepository extends JpaRepository<StudentPasswordLog, String> {
}

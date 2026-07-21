package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, String> {
    List<SchoolClass> findBySchoolId(String schoolId);
    long countBySchoolId(String schoolId);
}

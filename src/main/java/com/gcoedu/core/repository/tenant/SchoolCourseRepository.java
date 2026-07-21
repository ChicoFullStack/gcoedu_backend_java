package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.SchoolCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SchoolCourseRepository extends JpaRepository<SchoolCourse, UUID> {
    List<SchoolCourse> findBySchoolId(String schoolId);
}

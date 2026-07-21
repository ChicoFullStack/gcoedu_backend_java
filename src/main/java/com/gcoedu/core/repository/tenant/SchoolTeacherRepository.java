package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.SchoolTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SchoolTeacherRepository extends JpaRepository<SchoolTeacher, UUID> {
    long countBySchoolId(String schoolId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT st.school.id FROM SchoolTeacher st WHERE st.teacher.user.id = :userId")
    java.util.List<String> findSchoolIdsByTeacherUserId(
            @org.springframework.data.repository.query.Param("userId") String userId);
}

package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.TeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, String> {

    @Query("""
            SELECT tc.schoolClass.id
            FROM TeacherClass tc
            WHERE tc.teacher.user.id = :userId
            """)
    List<String> findClassIdsByTeacherUserId(@Param("userId") String userId);

    @Query("""
            SELECT tc.teacher
            FROM TeacherClass tc
            WHERE tc.schoolClass.id = :classId
            """)
    List<com.gcoedu.core.domain.entity.tenant.Teacher> findTeachersByClassId(@Param("classId") String classId);
}

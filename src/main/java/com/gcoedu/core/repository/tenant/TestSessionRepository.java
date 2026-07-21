package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, String> {
    List<TestSession> findByTestIdAndStudentIdAndStatusAndUserAgent(String testId, String studentId, String status, String userAgent);
    long countByTestId(String testId);
    long countByTestIdAndSubmittedAtIsNotNull(String testId);
    long countBySubmittedAtIsNotNull();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT s.student.id) FROM TestSession s WHERE s.submittedAt IS NOT NULL")
    long countDistinctStudentsWithSubmission();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.student.school.name FROM TestSession s WHERE s.test.id = :testId AND s.student.school IS NOT NULL")
    List<String> findDistinctSchoolNamesByTestId(
            @org.springframework.data.repository.query.Param("testId") String testId);
}

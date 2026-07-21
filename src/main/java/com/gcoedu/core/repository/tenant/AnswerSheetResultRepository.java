package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.AnswerSheetResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerSheetResultRepository extends JpaRepository<AnswerSheetResult, String> {
    List<AnswerSheetResult> findByGabaritoId(String gabaritoId);
    List<AnswerSheetResult> findByStudentId(String studentId);
    List<AnswerSheetResult> findByGabaritoTestId(String testId);

    @org.springframework.data.jpa.repository.Query("SELECT r.student, AVG(r.grade), COUNT(r.id) FROM AnswerSheetResult r GROUP BY r.student ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopStudentsRanking(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.student, AVG(r.grade), COUNT(r.id) FROM AnswerSheetResult r WHERE r.student.school.id = :schoolId GROUP BY r.student ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopStudentsRankingBySchool(
            @org.springframework.data.repository.query.Param("schoolId") String schoolId,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.student, AVG(r.grade), COUNT(r.id) FROM AnswerSheetResult r WHERE r.student.schoolClass.id = :classId GROUP BY r.student ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopStudentsRankingByClass(
            @org.springframework.data.repository.query.Param("classId") String classId,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.student, AVG(r.grade), COUNT(r.id) FROM AnswerSheetResult r WHERE r.student.school.id IN :schoolIds GROUP BY r.student ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopStudentsRankingBySchools(
            @org.springframework.data.repository.query.Param("schoolIds") java.util.Collection<String> schoolIds,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.student.schoolClass, AVG(r.grade), SUM(r.correctAnswersCount), AVG(r.scorePercentage), COUNT(DISTINCT r.gabarito.test.id), COUNT(DISTINCT r.student.id) FROM AnswerSheetResult r WHERE r.student.schoolClass IS NOT NULL GROUP BY r.student.schoolClass ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopClassesRanking(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.student.schoolClass, AVG(r.grade), SUM(r.correctAnswersCount), AVG(r.scorePercentage), COUNT(DISTINCT r.gabarito.test.id), COUNT(DISTINCT r.student.id) FROM AnswerSheetResult r WHERE r.student.schoolClass IS NOT NULL AND r.student.school.id = :schoolId GROUP BY r.student.schoolClass ORDER BY AVG(r.grade) DESC")
    org.springframework.data.domain.Page<Object[]> findTopClassesRankingBySchool(
            @org.springframework.data.repository.query.Param("schoolId") String schoolId,
            org.springframework.data.domain.Pageable pageable);
}

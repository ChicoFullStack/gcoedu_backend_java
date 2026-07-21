package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, String> {
    List<StudentAnswer> findByTestIdAndStudentId(String testId, String studentId);
    Optional<StudentAnswer> findByStudentIdAndTestIdAndQuestionId(String studentId, String testId, String questionId);
    boolean existsByQuestionId(String questionId);
}

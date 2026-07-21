package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, String> {
    List<EvaluationResult> findByTestId(String testId);
    java.util.Optional<EvaluationResult> findByTestIdAndStudentId(String testId, String studentId);
}

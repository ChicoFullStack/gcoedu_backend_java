package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, String> {
    List<TestQuestion> findByTestIdOrderByOrderIndex(String testId);
    void deleteByTestIdAndQuestionId(String testId, String questionId);
    long countByQuestionId(String questionId);

    @Query("""
            select tq.question.id, count(tq.id)
            from TestQuestion tq
            where tq.question.id in :questionIds
            group by tq.question.id
            """)
    List<Object[]> countByQuestionIds(@Param("questionIds") List<String> questionIds);
}

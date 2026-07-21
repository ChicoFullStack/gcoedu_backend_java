package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, String> {
    List<Question> findBySubjectId(String subjectId);

    org.springframework.data.domain.Page<Question> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"subject", "grade", "educationStage", "createdBy", "lastModifiedBy"})
    @Query("""
            select q
            from TenantQuestion q
            where (
                q.scopeType is null
                or q.scopeType = :globalScope
                or (q.scopeType = :cityScope and q.ownerCityId = :cityId)
                or (q.scopeType = :privateScope and q.ownerUserId = :userId)
            )
            and (:createdById is null or q.createdBy.id = :createdById)
            and (:subjectId is null or q.subject.id = :subjectId)
            and (:questionType is null or q.questionType = :questionType)
            order by q.createdAt desc, q.id
            """)
    List<Question> findAccessible(
            @Param("globalScope") String globalScope,
            @Param("cityScope") String cityScope,
            @Param("privateScope") String privateScope,
            @Param("cityId") String cityId,
            @Param("userId") String userId,
            @Param("createdById") String createdById,
            @Param("subjectId") String subjectId,
            @Param("questionType") String questionType
    );

    @Query("select coalesce(max(q.number), 0) from TenantQuestion q")
    int findMaxNumber();
}

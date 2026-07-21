package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {
    java.util.List<Grade> findByEducationStageId(UUID educationStageId);
}

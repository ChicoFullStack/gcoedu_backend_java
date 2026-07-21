package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.EducationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EducationStageRepository extends JpaRepository<EducationStage, UUID> {
}

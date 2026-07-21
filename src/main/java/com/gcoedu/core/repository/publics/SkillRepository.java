package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findAllByOrderByCodeAsc();
    List<Skill> findDistinctByGradesIdOrderByCodeAsc(UUID gradeId);
    List<Skill> findBySubjectIdOrderByCodeAsc(String subjectId);
    List<Skill> findDistinctBySubjectIdAndGradesIdOrderByCodeAsc(String subjectId, UUID gradeId);
    java.util.Optional<Skill> findByCodeIgnoreCase(String code);
}

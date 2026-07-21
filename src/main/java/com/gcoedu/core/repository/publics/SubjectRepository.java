package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {
    Optional<Subject> findByNameIgnoreCase(String name);
}

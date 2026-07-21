package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, String> {
    List<Test> findByStatus(String status);
    
    org.springframework.data.domain.Page<Test> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Test> findByTypeIgnoreCaseOrderByCreatedAtDesc(
            String type, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Test> findByTypeIgnoreCaseAndCreatorIdOrderByCreatedAtDesc(
            String type, String creatorId, org.springframework.data.domain.Pageable pageable);
    long countByCreatorId(String creatorId);
}

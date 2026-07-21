package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolRepository extends JpaRepository<School, String> {
    boolean existsByInepCode(String inepCode);
    java.util.List<School> findByCityId(String cityId);
}

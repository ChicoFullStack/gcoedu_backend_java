package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, String> {
    Optional<Manager> findByUserId(String userId);
    List<Manager> findBySchoolId(String schoolId);
    long countAllBySchoolId(String schoolId);

    @Query("SELECT m FROM Manager m JOIN m.user u WHERE u.city.id = :cityId AND u.role = :role")
    List<Manager> findByCityIdAndUserRole(@Param("cityId") String cityId, @Param("role") com.gcoedu.core.domain.entity.publics.RoleEnum role);
}

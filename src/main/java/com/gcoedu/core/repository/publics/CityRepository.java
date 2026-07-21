package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, String> {
    Optional<City> findBySlug(String slug);
}

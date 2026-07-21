package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByRegistration(String registration);
    List<User> findByCityId(String cityId);
    long countByCityId(String cityId);
}

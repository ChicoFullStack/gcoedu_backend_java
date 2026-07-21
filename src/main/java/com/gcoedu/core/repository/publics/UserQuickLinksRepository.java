package com.gcoedu.core.repository.publics;

import com.gcoedu.core.domain.entity.publics.UserQuickLinks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQuickLinksRepository extends JpaRepository<UserQuickLinks, String> {
    List<UserQuickLinks> findByUserId(String userId);
}

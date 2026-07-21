package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.PlayTvVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayTvRepository extends JpaRepository<PlayTvVideo, String> {
    List<PlayTvVideo> findAllByOrderByCreatedAtDesc();
}

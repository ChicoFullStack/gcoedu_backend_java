package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, String> {
    List<CalendarEvent> findAllByOrderByStartAtAsc();

    List<CalendarEvent> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            OffsetDateTime rangeEnd,
            OffsetDateTime rangeStart
    );
}

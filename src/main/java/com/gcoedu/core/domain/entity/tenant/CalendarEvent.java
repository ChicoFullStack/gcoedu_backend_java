package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
@Data
@NoArgsConstructor
public class CalendarEvent {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "all_day")
    private boolean allDay;

    @Column(length = 50)
    private String timezone;

    @Column(name = "visibility_scope", length = 50)
    private String visibilityScope;

    @Column(name = "created_by_user_id", length = 36)
    private String createdByUserId;

    @Column(name = "created_by_role", length = 32)
    private String createdByRole;

    @Column(name = "is_published")
    private boolean isPublished;

    @Column(length = 255)
    private String location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CalendarEventTarget> targets = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    private List<CalendarEventResource> resources = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "recurrence_rule", length = 255)
    private String recurrenceRule;

    @Column(name = "municipality_id", length = 36)
    private String municipalityId;

    @Column(name = "school_id", length = 36)
    private String schoolId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void setLastUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_event_resources")
@Data
@NoArgsConstructor
public class CalendarEventResource {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private CalendarEvent event;

    @Column(name = "resource_type", nullable = false, length = 16)
    private String resourceType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2048)
    private String url;

    @Column(name = "minio_bucket", length = 100)
    private String minioBucket;

    @Column(name = "minio_object_name", length = 1024)
    private String minioObjectName;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 160)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

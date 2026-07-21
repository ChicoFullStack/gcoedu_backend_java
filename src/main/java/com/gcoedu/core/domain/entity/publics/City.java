package com.gcoedu.core.domain.entity.publics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "city", schema = "public")
@Data
@NoArgsConstructor
public class City {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 100)
    private String name;

    @Column(length = 100, nullable = false)
    private String state;

    @Column(length = 100, unique = true, nullable = false)
    private String slug;

    @Column(name = "plan_code", length = 20, nullable = false)
    private String planCode = "basic";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "letterhead_image_url", columnDefinition = "TEXT")
    private String letterheadImageUrl;

    @Column(name = "letterhead_pdf_url", columnDefinition = "TEXT")
    private String letterheadPdfUrl;
}

package com.gcoedu.core.domain.entity.publics;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings", schema = "public")
@Data
@NoArgsConstructor
public class UserSettings {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50)
    private String theme;

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "font_size")
    private Integer fontSize;

    @Column(name = "sidebar_theme_id", length = 128)
    private String sidebarThemeId;

    @Column(name = "frame_id", length = 128)
    private String frameId;

    @Column(name = "stamp_id", length = 128)
    private String stampId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void setLastUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

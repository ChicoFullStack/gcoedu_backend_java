package com.gcoedu.core.domain.entity.tenant;

import com.gcoedu.core.domain.entity.publics.City;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school")
@Data
@NoArgsConstructor
public class School {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 100)
    private String name;

    @Column(name = "inep_code", length = 20, unique = true)
    private String inepCode;

    @Column(length = 200)
    private String address;

    @Column(length = 100)
    private String domain;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
}

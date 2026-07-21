package com.gcoedu.core.domain.entity.tenant;

import com.gcoedu.core.domain.entity.publics.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher")
@Data
@NoArgsConstructor
public class Teacher {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(length = 50, unique = true)
    private String registration;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}

package com.gcoedu.core.domain.entity.tenant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_password_log")
@Data
@NoArgsConstructor
public class StudentPasswordLog {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "student_name", length = 100, nullable = false)
    private String studentName;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String registration;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "grade_id")
    private UUID gradeId;

    @Column(name = "school_id", length = 36)
    private String schoolId;

    @Column(name = "city_id")
    private String cityId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

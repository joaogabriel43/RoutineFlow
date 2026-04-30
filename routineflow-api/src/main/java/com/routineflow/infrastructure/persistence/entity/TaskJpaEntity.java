package com.routineflow.infrastructure.persistence.entity;

import com.routineflow.domain.model.ScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private AreaJpaEntity area;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    /** Non-null for DAY_OF_WEEK tasks; null for DAY_OF_MONTH tasks. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 15)
    @Builder.Default
    private ScheduleType scheduleType = ScheduleType.DAY_OF_WEEK;

    /** Non-null for DAY_OF_MONTH tasks (1-31); null for DAY_OF_WEEK tasks. */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

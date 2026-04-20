package com.routineflow.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "scheduler_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "users_processed")
    private Integer usersProcessed;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}

package com.routineflow.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AreaJpaEntity> areas = new ArrayList<>();
}

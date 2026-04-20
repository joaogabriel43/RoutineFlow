package com.routineflow.application.usecase;

import com.routineflow.application.dto.StreakListResponse;
import com.routineflow.application.dto.StreakResponse;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetStreakUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final StreakJpaRepository streakJpaRepository;

    public GetStreakUseCase(RoutineJpaRepository routineJpaRepository,
                            AreaJpaRepository areaJpaRepository,
                            StreakJpaRepository streakJpaRepository) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.streakJpaRepository = streakJpaRepository;
    }

    @Transactional(readOnly = true)
    public StreakListResponse getStreaks(Long userId) {
        var routineOpt = routineJpaRepository.findActiveByUserId(userId);
        if (routineOpt.isEmpty()) {
            return new StreakListResponse(List.of());
        }

        var routine = routineOpt.get();
        var areas = areaJpaRepository.findByRoutineId(routine.getId());

        Map<Long, StreakJpaEntity> streaksByAreaId = streakJpaRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(s -> s.getArea().getId(), s -> s));

        List<StreakResponse> streaks = areas.stream()
                .map(area -> {
                    var streak = streaksByAreaId.get(area.getId());
                    return new StreakResponse(
                            area.getId(),
                            area.getName(),
                            area.getColor(),
                            area.getIcon(),
                            streak != null ? streak.getCurrentCount() : 0,
                            streak != null ? streak.getLastActiveDate() : null
                    );
                })
                .sorted(Comparator.comparingInt(StreakResponse::currentStreak).reversed())
                .collect(Collectors.toList());

        return new StreakListResponse(streaks);
    }
}

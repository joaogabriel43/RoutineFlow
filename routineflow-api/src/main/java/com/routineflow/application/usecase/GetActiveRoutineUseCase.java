package com.routineflow.application.usecase;

import com.routineflow.application.dto.RoutineResponse;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetActiveRoutineUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;

    public GetActiveRoutineUseCase(
            RoutineJpaRepository routineJpaRepository,
            AreaJpaRepository areaJpaRepository
    ) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
    }

    @Transactional(readOnly = true)
    public RoutineResponse execute(Long userId) {
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ActiveRoutineNotFoundException(userId));

        int totalAreas = routine.getAreas().size();
        int totalTasks = routine.getAreas().stream()
                .mapToInt(a -> a.getTasks().size())
                .sum();

        return new RoutineResponse(routine.getId(), routine.getName(),
                routine.getImportedAt(), totalAreas, totalTasks);
    }

    public static class ActiveRoutineNotFoundException extends RuntimeException {
        public ActiveRoutineNotFoundException(Long userId) {
            super("No active routine found for user: " + userId);
        }
    }
}

package com.routineflow.application.usecase;

import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecoverRoutineUseCase {

    private final RoutineJpaRepository routineJpaRepository;

    public RecoverRoutineUseCase(RoutineJpaRepository routineJpaRepository) {
        this.routineJpaRepository = routineJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllRoutines(Long userId) {
        return routineJpaRepository.findAllByUserId(userId).stream()
                .map(r -> Map.<String, Object>of(
                        "id",         r.getId(),
                        "name",       r.getName(),
                        "active",     r.isActive(),
                        "importedAt", r.getImportedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> activateRoutine(Long userId, Long id) {
        // Deactivate all routines belonging to this user
        routineJpaRepository.deactivateAllByUserId(userId);

        // Activate the requested routine (ownership enforced by userId check)
        RoutineJpaEntity routine = routineJpaRepository.findAllByUserId(userId).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Routine not found: " + id));

        routine.setActive(true);
        routineJpaRepository.save(routine);

        return Map.of(
                "id",         routine.getId(),
                "name",       routine.getName(),
                "active",     routine.isActive(),
                "importedAt", routine.getImportedAt()
        );
    }
}

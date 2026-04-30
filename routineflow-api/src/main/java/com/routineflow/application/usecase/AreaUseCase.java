package com.routineflow.application.usecase;

import com.routineflow.application.dto.AreaResponse;
import com.routineflow.application.dto.CreateAreaRequest;
import com.routineflow.application.dto.ReorderAreasRequest;
import com.routineflow.application.dto.TaskResponse;
import com.routineflow.application.dto.UpdateAreaRequest;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.domain.model.ResetFrequency;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaUseCase {

    private final AreaJpaRepository areaJpaRepository;
    private final RoutineJpaRepository routineJpaRepository;

    public AreaUseCase(
            AreaJpaRepository areaJpaRepository,
            RoutineJpaRepository routineJpaRepository
    ) {
        this.areaJpaRepository = areaJpaRepository;
        this.routineJpaRepository = routineJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> getAreas(Long userId) {
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active routine found for user: " + userId));

        return areaJpaRepository
                .findAreasWithTasksByRoutineIdOrderByOrderIndex(routine.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AreaResponse createArea(Long userId, CreateAreaRequest request) {
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active routine found for user: " + userId));

        // New area gets the next orderIndex
        int nextOrder = areaJpaRepository.findByRoutineId(routine.getId()).size();

        var frequency = request.resetFrequency() != null ? request.resetFrequency() : ResetFrequency.DAILY;

        var area = AreaJpaEntity.builder()
                .user(routine.getUser())
                .routine(routine)
                .name(request.name())
                .color(request.color())
                .icon(request.icon())
                .orderIndex(nextOrder)
                .resetFrequency(frequency)
                .build();

        return toResponse(areaJpaRepository.save(area));
    }

    @Transactional
    public AreaResponse updateArea(Long userId, Long areaId, UpdateAreaRequest request) {
        var area = areaJpaRepository.findByIdAndUserId(areaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found: " + areaId));

        area.setName(request.name());
        area.setColor(request.color());
        area.setIcon(request.icon());
        if (request.resetFrequency() != null) {
            area.setResetFrequency(request.resetFrequency());
        }

        return toResponse(areaJpaRepository.save(area));
    }

    @Transactional
    public void deleteArea(Long userId, Long areaId) {
        var area = areaJpaRepository.findByIdAndUserId(areaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found: " + areaId));

        // CascadeType.ALL on tasks — JPA handles cascade delete automatically
        areaJpaRepository.deleteById(area.getId());
    }

    @Transactional
    public List<AreaResponse> reorderAreas(Long userId, ReorderAreasRequest request) {
        var areas = areaJpaRepository.findAllById(request.areaIds());

        // Validate all areas belong to the requesting user
        for (var area : areas) {
            if (!area.getUser().getId().equals(userId)) {
                throw new UnauthorizedException(
                        "User " + userId + " does not own area " + area.getId());
            }
        }

        // Build a lookup map to preserve the requested order from the IDs list
        var areaById = areas.stream()
                .collect(java.util.stream.Collectors.toMap(AreaJpaEntity::getId, a -> a));

        // Apply orderIndex based on position in the requested list
        var toSave = new java.util.ArrayList<AreaJpaEntity>();
        for (int i = 0; i < request.areaIds().size(); i++) {
            var area = areaById.get(request.areaIds().get(i));
            if (area != null) {
                area.setOrderIndex(i);
                toSave.add(area);
            }
        }

        areaJpaRepository.saveAll(toSave);

        // Return updated ordered list
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active routine found for user: " + userId));

        return areaJpaRepository
                .findAreasWithTasksByRoutineIdOrderByOrderIndex(routine.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private AreaResponse toResponse(AreaJpaEntity area) {
        var tasks = area.getTasks() == null
                ? List.<TaskResponse>of()
                : area.getTasks().stream()
                        .map(t -> new TaskResponse(
                                t.getId(),
                                t.getTitle(),
                                t.getDescription(),
                                t.getEstimatedMinutes(),
                                t.getOrderIndex(),
                                t.getScheduleType(),
                                t.getDayOfWeek(),
                                t.getDayOfMonth()))
                        .sorted(java.util.Comparator.comparingInt(TaskResponse::orderIndex))
                        .toList();

        return new AreaResponse(
                area.getId(),
                area.getName(),
                area.getColor(),
                area.getIcon(),
                area.getOrderIndex(),
                area.getResetFrequency() != null ? area.getResetFrequency() : ResetFrequency.DAILY,
                tasks
        );
    }
}

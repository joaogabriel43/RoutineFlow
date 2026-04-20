package com.routineflow.application.usecase;

import com.routineflow.application.dto.AreaWithTasksResponse;
import com.routineflow.application.dto.DayScheduleResponse;
import com.routineflow.application.dto.TaskResponse;
import com.routineflow.application.usecase.GetActiveRoutineUseCase.ActiveRoutineNotFoundException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

@Service
public class GetDayScheduleUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;

    public GetDayScheduleUseCase(
            RoutineJpaRepository routineJpaRepository,
            AreaJpaRepository areaJpaRepository
    ) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
    }

    @Transactional(readOnly = true)
    public DayScheduleResponse execute(Long userId, DayOfWeek dayOfWeek) {
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ActiveRoutineNotFoundException(userId));

        // JOIN FETCH no repositório — zero N+1
        List<AreaJpaEntity> areas = areaJpaRepository
                .findAreasWithTasksByRoutineIdAndDay(routine.getId(), dayOfWeek);

        List<AreaWithTasksResponse> areaResponses = areas.stream()
                .map(area -> toAreaResponse(area, dayOfWeek))
                .toList();

        return new DayScheduleResponse(dayOfWeek, areaResponses);
    }

    private AreaWithTasksResponse toAreaResponse(AreaJpaEntity area, DayOfWeek dayOfWeek) {
        List<TaskResponse> tasks = area.getTasks().stream()
                .filter(t -> t.getDayOfWeek() == dayOfWeek)
                .sorted(Comparator.comparingInt(TaskJpaEntity::getOrderIndex))
                .map(t -> new TaskResponse(t.getId(), t.getTitle(), t.getDescription(),
                        t.getEstimatedMinutes(), t.getOrderIndex(), t.getDayOfWeek()))
                .toList();

        return new AreaWithTasksResponse(area.getId(), area.getName(),
                area.getColor(), area.getIcon(), tasks);
    }
}

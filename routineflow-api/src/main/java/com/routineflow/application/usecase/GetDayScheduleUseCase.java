package com.routineflow.application.usecase;

import com.routineflow.application.dto.AreaWithTasksResponse;
import com.routineflow.application.dto.DayScheduleResponse;
import com.routineflow.application.dto.TaskResponse;
import com.routineflow.application.usecase.GetActiveRoutineUseCase.ActiveRoutineNotFoundException;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    public DayScheduleResponse execute(Long userId, LocalDate date) {
        var routine = routineJpaRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ActiveRoutineNotFoundException(userId));

        // Load all tasks for all areas — filter in Java to support both DAY_OF_WEEK and DAY_OF_MONTH
        List<AreaJpaEntity> allAreas = areaJpaRepository
                .findAreasWithTasksByRoutineIdOrderByOrderIndex(routine.getId());

        List<AreaWithTasksResponse> areaResponses = allAreas.stream()
                .map(area -> toAreaResponse(area, date))
                .filter(a -> !a.tasks().isEmpty()) // exclude areas with no applicable tasks
                .toList();

        return new DayScheduleResponse(date.getDayOfWeek(), areaResponses);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AreaWithTasksResponse toAreaResponse(AreaJpaEntity area, LocalDate date) {
        List<TaskResponse> tasks = area.getTasks().stream()
                .filter(t -> taskAppliesOnDate(t, date))
                .sorted(Comparator.comparingInt(TaskJpaEntity::getOrderIndex))
                .map(t -> new TaskResponse(
                        t.getId(), t.getTitle(), t.getDescription(),
                        t.getEstimatedMinutes(), t.getOrderIndex(),
                        t.getScheduleType(), t.getDayOfWeek(), t.getDayOfMonth()))
                .toList();

        return new AreaWithTasksResponse(area.getId(), area.getName(),
                area.getColor(), area.getIcon(), tasks);
    }

    /**
     * Returns true when the task should appear in the schedule for the given date.
     *
     * DAY_OF_WEEK  → task.dayOfWeek matches date.dayOfWeek
     * DAY_OF_MONTH → task.dayOfMonth matches date.dayOfMonth AND the month has that day
     *                (e.g. day 31 does not appear in months with fewer days)
     */
    public static boolean taskAppliesOnDate(TaskJpaEntity task, LocalDate date) {
        return switch (task.getScheduleType()) {
            case DAY_OF_WEEK -> task.getDayOfWeek() == date.getDayOfWeek();
            case DAY_OF_MONTH -> {
                int lastDay = date.lengthOfMonth();
                yield task.getDayOfMonth() != null
                        && task.getDayOfMonth() <= lastDay
                        && task.getDayOfMonth() == date.getDayOfMonth();
            }
        };
    }
}

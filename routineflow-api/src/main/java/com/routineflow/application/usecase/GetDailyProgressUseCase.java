package com.routineflow.application.usecase;

import com.routineflow.application.dto.AreaProgressResponse;
import com.routineflow.application.dto.DailyProgressResponse;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetDailyProgressUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;

    public GetDailyProgressUseCase(
            RoutineJpaRepository routineJpaRepository,
            AreaJpaRepository areaJpaRepository,
            DailyLogJpaRepository dailyLogJpaRepository
    ) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
    }

    @Transactional(readOnly = true)
    public DailyProgressResponse getProgress(Long userId, LocalDate date) {
        var routineOpt = routineJpaRepository.findActiveByUserId(userId);
        if (routineOpt.isEmpty()) {
            return new DailyProgressResponse(date, List.of(), 0.0);
        }

        var routine = routineOpt.get();
        var dayOfWeek = date.getDayOfWeek();

        // Busca tarefas do dia via JOIN FETCH — zero N+1
        var areas = areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(routine.getId(), dayOfWeek);

        // Busca todos os logs do usuário para o dia em uma única query
        var logs = dailyLogJpaRepository.findAllByUserIdAndLogDate(userId, date);
        Set<Long> completedTaskIds = logs.stream()
                .filter(DailyLogJpaEntity::isCompleted)
                .map(l -> l.getTask().getId())
                .collect(Collectors.toSet());

        List<AreaProgressResponse> areaResponses = areas.stream()
                .map(area -> {
                    List<TaskJpaEntity> dayTasks = area.getTasks().stream()
                            .filter(t -> t.getDayOfWeek() == dayOfWeek)
                            .toList();
                    int total = dayTasks.size();
                    int completed = (int) dayTasks.stream()
                            .filter(t -> completedTaskIds.contains(t.getId()))
                            .count();
                    double rate = total == 0 ? 0.0 : (double) completed / total;
                    return new AreaProgressResponse(
                            area.getId(), area.getName(), area.getColor(), area.getIcon(),
                            total, completed, rate
                    );
                })
                .toList();

        int totalTasks = areaResponses.stream().mapToInt(AreaProgressResponse::totalTasks).sum();
        int totalCompleted = areaResponses.stream().mapToInt(AreaProgressResponse::completedTasks).sum();
        double overall = totalTasks == 0 ? 0.0 : (double) totalCompleted / totalTasks;

        return new DailyProgressResponse(date, areaResponses, overall);
    }
}

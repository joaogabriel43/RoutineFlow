package com.routineflow.application.usecase;

import com.routineflow.application.dto.ImportRoutineResponse;
import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedTask;
import com.routineflow.infrastructure.parser.RoutineFileParser;
import com.routineflow.infrastructure.parser.RoutineParseException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Service
public class ImportRoutineUseCase {

    private final List<RoutineFileParser> parsers;
    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public ImportRoutineUseCase(
            List<RoutineFileParser> parsers,
            RoutineJpaRepository routineJpaRepository,
            AreaJpaRepository areaJpaRepository,
            TaskJpaRepository taskJpaRepository,
            UserJpaRepository userJpaRepository
    ) {
        this.parsers = parsers;
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional
    public ImportRoutineResponse execute(Long userId, InputStream inputStream, String fileExtension) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var parser = parsers.stream()
                .filter(p -> p.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No parser found for file extension: " + fileExtension));

        // Lança RoutineParseException se inválido — @Transactional faz rollback automaticamente
        var parsed = parser.parse(inputStream);

        // Desativa todas as rotinas ativas do usuário antes de criar a nova
        routineJpaRepository.deactivateAllByUserId(userId);

        var routine = RoutineJpaEntity.builder()
                .user(user)
                .name(parsed.name())
                .active(true)
                .build();

        routine = routineJpaRepository.save(routine);

        int totalTasks = 0;
        for (ParsedArea parsedArea : parsed.areas()) {
            var area = AreaJpaEntity.builder()
                    .user(user)
                    .routine(routine)
                    .name(parsedArea.name())
                    .color(parsedArea.color())
                    .icon(parsedArea.icon())
                    .build();

            area = areaJpaRepository.save(area);
            totalTasks += persistTasks(area, parsedArea.schedule());
        }

        return new ImportRoutineResponse(
                routine.getId(),
                routine.getName(),
                parsed.areas().size(),
                totalTasks,
                routine.getImportedAt()
        );
    }

    private int persistTasks(AreaJpaEntity area, Map<DayOfWeek, List<ParsedTask>> schedule) {
        int count = 0;
        for (var entry : schedule.entrySet()) {
            for (ParsedTask parsedTask : entry.getValue()) {
                var task = TaskJpaEntity.builder()
                        .area(area)
                        .title(parsedTask.title())
                        .description(parsedTask.description())
                        .estimatedMinutes(parsedTask.estimatedMinutes())
                        .dayOfWeek(entry.getKey())
                        .orderIndex(parsedTask.orderIndex())
                        .build();
                taskJpaRepository.save(task);
                count++;
            }
        }
        return count;
    }
}

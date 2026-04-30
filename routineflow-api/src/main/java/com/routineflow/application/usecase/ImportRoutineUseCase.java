package com.routineflow.application.usecase;

import com.routineflow.application.dto.ImportRoutineResponse;
import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedTask;
import com.routineflow.application.dto.ParsedRoutine;
import com.routineflow.domain.model.ImportMode;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.parser.RoutineFileParser;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ImportRoutineResponse execute(Long userId, InputStream inputStream,
                                        String fileExtension, ImportMode mode) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var parser = parsers.stream()
                .filter(p -> p.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No parser found for file extension: " + fileExtension));

        // Throws RoutineParseException if file is invalid — @Transactional rolls back automatically
        var parsed = parser.parse(inputStream);

        if (mode == ImportMode.MERGE) {
            var activeOpt = routineJpaRepository.findActiveByUserId(userId);
            if (activeOpt.isPresent()) {
                return executeMerge(user, parsed, activeOpt.get());
            }
            // No active routine → fall through to REPLACE behaviour, but keep mode=MERGE in response
        }

        return executeReplace(user, parsed, mode);
    }

    // ── REPLACE ───────────────────────────────────────────────────────────────

    private ImportRoutineResponse executeReplace(UserJpaEntity user, ParsedRoutine parsed, ImportMode mode) {
        routineJpaRepository.deactivateAllByUserId(user.getId());

        var routine = RoutineJpaEntity.builder()
                .user(user)
                .name(parsed.name())
                .active(true)
                .build();
        routine = routineJpaRepository.save(routine);

        int areasCreated = 0;
        int tasksCreated = 0;

        for (ParsedArea parsedArea : parsed.areas()) {
            var area = AreaJpaEntity.builder()
                    .user(user)
                    .routine(routine)
                    .name(parsedArea.name())
                    .color(parsedArea.color())
                    .icon(parsedArea.icon())
                    .orderIndex(areasCreated)
                    .build();

            area = areaJpaRepository.save(area);
            tasksCreated += persistTasks(area, parsedArea.schedule());
            areasCreated++;
        }

        return new ImportRoutineResponse(
                routine.getId(),
                routine.getName(),
                areasCreated,
                tasksCreated,
                routine.getImportedAt(),
                mode,
                areasCreated,
                0,
                tasksCreated,
                0
        );
    }

    // ── MERGE ─────────────────────────────────────────────────────────────────

    private ImportRoutineResponse executeMerge(UserJpaEntity user, ParsedRoutine parsed,
                                               RoutineJpaEntity activeRoutine) {
        var existingAreas = areaJpaRepository
                .findAreasWithTasksByRoutineIdOrderByOrderIndex(activeRoutine.getId());

        // Case-insensitive name → existing area map
        Map<String, AreaJpaEntity> nameToArea = existingAreas.stream()
                .collect(Collectors.toMap(
                        a -> a.getName().trim().toLowerCase(),
                        a -> a
                ));

        int areasCreated  = 0;
        int areasMerged   = 0;
        int tasksCreated  = 0;
        int tasksSkipped  = 0;
        int nextAreaOrder = existingAreas.size();

        for (ParsedArea parsedArea : parsed.areas()) {
            String normalizedName = parsedArea.name().trim().toLowerCase();

            if (!nameToArea.containsKey(normalizedName)) {
                // Completely new area — create with all its tasks
                var area = AreaJpaEntity.builder()
                        .user(user)
                        .routine(activeRoutine)
                        .name(parsedArea.name())
                        .color(parsedArea.color())
                        .icon(parsedArea.icon())
                        .orderIndex(nextAreaOrder++)
                        .build();
                area = areaJpaRepository.save(area);
                tasksCreated += persistTasks(area, parsedArea.schedule());
                areasCreated++;

            } else {
                // Area already exists — merge tasks
                var existingArea = nameToArea.get(normalizedName);
                var currentTasks = taskJpaRepository.findByAreaIdOrderByOrderIndex(existingArea.getId());

                // Dedup key: title (case-insensitive) + scheduleType + dayOfWeek + dayOfMonth
                Set<String> existingKeys = currentTasks.stream()
                        .map(this::taskKey)
                        .collect(Collectors.toSet());

                int nextTaskOrder = currentTasks.size();

                for (Map.Entry<DayOfWeek, List<ParsedTask>> entry : parsedArea.schedule().entrySet()) {
                    DayOfWeek day = entry.getKey();
                    for (ParsedTask parsedTask : entry.getValue()) {
                        // YAML-imported tasks are always DAY_OF_WEEK
                        String key = parsedTask.title().trim().toLowerCase()
                                + "|DAY_OF_WEEK|" + day.name() + "|null";

                        if (existingKeys.contains(key)) {
                            tasksSkipped++;
                        } else {
                            var task = TaskJpaEntity.builder()
                                    .area(existingArea)
                                    .title(parsedTask.title())
                                    .description(parsedTask.description())
                                    .estimatedMinutes(parsedTask.estimatedMinutes())
                                    .dayOfWeek(day)
                                    .scheduleType(ScheduleType.DAY_OF_WEEK)
                                    .orderIndex(nextTaskOrder++)
                                    .build();
                            taskJpaRepository.save(task);
                            tasksCreated++;
                        }
                    }
                }
                areasMerged++;
            }
        }

        return new ImportRoutineResponse(
                activeRoutine.getId(),
                activeRoutine.getName(),
                areasCreated + areasMerged,
                tasksCreated + tasksSkipped,
                Instant.now(),
                ImportMode.MERGE,
                areasCreated,
                areasMerged,
                tasksCreated,
                tasksSkipped
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Persists all tasks for a given area from the parsed schedule map.
     * YAML-imported tasks are always DAY_OF_WEEK — scheduleType defaults to that via @Builder.Default.
     */
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

    /**
     * Builds a dedup key for a task: title|scheduleType|dayOfWeek|dayOfMonth.
     * Used in MERGE mode to detect exact duplicates.
     */
    private String taskKey(TaskJpaEntity task) {
        return task.getTitle().trim().toLowerCase()
                + "|" + (task.getScheduleType() != null ? task.getScheduleType().name() : "DAY_OF_WEEK")
                + "|" + (task.getDayOfWeek() != null ? task.getDayOfWeek().name() : "null")
                + "|" + (task.getDayOfMonth() != null ? task.getDayOfMonth() : "null");
    }
}

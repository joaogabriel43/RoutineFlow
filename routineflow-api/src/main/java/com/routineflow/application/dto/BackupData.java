package com.routineflow.application.dto;

import java.util.List;

public record BackupData(
    String exportedAt,
    String version,
    List<RoutineBackup> routines,
    List<SingleTaskBackup> singleTasks,
    List<DailyLogBackup> dailyLogs
) {
    public record RoutineBackup(
        Long id,
        String name,
        boolean active,
        List<AreaBackup> areas
    ) {}

    public record AreaBackup(
        Long id,
        String name,
        String color,
        String icon,
        String resetFrequency,
        List<TaskBackup> tasks
    ) {}

    public record TaskBackup(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        String scheduleType,
        String dayOfWeek,
        Integer dayOfMonth
    ) {}

    public record SingleTaskBackup(
        Long id,
        String title,
        String description,
        String deadline,
        boolean completed,
        String completedAt
    ) {}

    public record DailyLogBackup(
        Long id,
        Long taskId,
        String logDate,
        boolean completed,
        String completedAt
    ) {}
}

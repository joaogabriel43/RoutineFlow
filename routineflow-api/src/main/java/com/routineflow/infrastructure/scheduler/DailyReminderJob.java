package com.routineflow.infrastructure.scheduler;

import com.routineflow.application.usecase.GetDayScheduleUseCase;
import com.routineflow.application.usecase.PushNotificationService;
import com.routineflow.infrastructure.config.AppTimeZone;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DailyReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReminderJob.class);

    private final TaskJpaRepository taskJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;
    private final PushNotificationService pushNotificationService;

    public DailyReminderJob(
            TaskJpaRepository taskJpaRepository,
            DailyLogJpaRepository dailyLogJpaRepository,
            PushNotificationService pushNotificationService
    ) {
        this.taskJpaRepository = taskJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Runs every minute. For each task with a reminderTime matching the current HH:mm:
     * 1. Check if the task applies on today's date (DAY_OF_WEEK / DAY_OF_MONTH)
     * 2. Check if the task has already been completed today
     * 3. Send a push notification if both conditions pass
     */
    @Scheduled(cron = "0 * * * * *", zone = "America/Sao_Paulo")
    public void execute() {
        LocalDate today = LocalDate.now(AppTimeZone.ZONE);
        LocalTime now = LocalTime.now(AppTimeZone.ZONE);
        // Truncate to minute precision for matching
        LocalTime currentMinute = LocalTime.of(now.getHour(), now.getMinute());

        log.debug("[DailyReminderJob] Checking reminders for time={} date={}", currentMinute, today);

        List<TaskJpaEntity> tasksWithReminder = taskJpaRepository.findByReminderTime(currentMinute);
        if (tasksWithReminder.isEmpty()) {
            return;
        }

        int sent = 0;
        for (TaskJpaEntity task : tasksWithReminder) {
            // 1. Does this task apply today?
            if (!GetDayScheduleUseCase.taskAppliesOnDate(task, today)) {
                continue;
            }

            Long userId = task.getArea().getUser().getId();

            // 2. Already completed today? Skip
            if (isTaskCompletedToday(userId, task.getId(), today)) {
                continue;
            }

            // 3. Send push
            String areaName = task.getArea().getName();
            pushNotificationService.sendNotification(
                    userId,
                    task.getTitle(),
                    "Hora de fazer: " + areaName
            );
            sent++;
        }

        if (sent > 0) {
            log.info("[DailyReminderJob] Sent {} reminder(s) at {}", sent, currentMinute);
        }
    }

    private boolean isTaskCompletedToday(Long userId, Long taskId, LocalDate date) {
        var logs = dailyLogJpaRepository.findAllByUserIdAndLogDate(userId, date);
        return logs.stream()
                .filter(DailyLogJpaEntity::isCompleted)
                .anyMatch(l -> l.getTask().getId().equals(taskId));
    }
}

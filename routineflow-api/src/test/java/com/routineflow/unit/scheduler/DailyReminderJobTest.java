package com.routineflow.unit.scheduler;

import com.routineflow.application.usecase.PushNotificationService;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import com.routineflow.infrastructure.scheduler.DailyReminderJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReminderJobTest {

    @Mock private TaskJpaRepository taskJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;
    @Mock private PushNotificationService pushNotificationService;

    private DailyReminderJob job;

    private static final Long USER_ID = 1L;
    // Fixed Monday for deterministic tests
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 15);
    private static final LocalTime SEVEN_THIRTY = LocalTime.of(7, 30);

    @BeforeEach
    void setUp() {
        job = new DailyReminderJob(taskJpaRepository, dailyLogJpaRepository, pushNotificationService);
    }

    @Test
    @DisplayName("execute_taskWithMatchingReminderTime_sendsNotification")
    void execute_taskWithMatchingReminderTime_sendsNotification() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var area = AreaJpaEntity.builder().id(1L).name("English").user(user).build();
        var task = TaskJpaEntity.builder()
                .id(10L).title("Practice Speaking").area(area)
                .scheduleType(ScheduleType.DAY_OF_WEEK).dayOfWeek(DayOfWeek.MONDAY)
                .reminderTime(SEVEN_THIRTY).orderIndex(0).build();

        when(taskJpaRepository.findByReminderTime(SEVEN_THIRTY)).thenReturn(List.of(task));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, MONDAY)).thenReturn(List.of());

        // Mock static clocks to return MONDAY at 07:30
        try (MockedStatic<LocalDate> dateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
             MockedStatic<LocalTime> timeMock = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            dateMock.when(() -> LocalDate.now(any(ZoneId.class))).thenReturn(MONDAY);
            timeMock.when(() -> LocalTime.now(any(ZoneId.class))).thenReturn(SEVEN_THIRTY);

            job.execute();
        }

        verify(pushNotificationService).sendNotification(
                eq(USER_ID), eq("Practice Speaking"), eq("Hora de fazer: English"));
    }

    @Test
    @DisplayName("execute_taskWithDifferentReminderTime_doesNotSend")
    void execute_taskWithDifferentReminderTime_doesNotSend() {
        LocalTime sevenThirtyOne = LocalTime.of(7, 31);

        // At 07:31, query for tasks with reminderTime=07:31 returns empty
        when(taskJpaRepository.findByReminderTime(sevenThirtyOne)).thenReturn(List.of());

        try (MockedStatic<LocalDate> dateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
             MockedStatic<LocalTime> timeMock = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            dateMock.when(() -> LocalDate.now(any(ZoneId.class))).thenReturn(MONDAY);
            timeMock.when(() -> LocalTime.now(any(ZoneId.class))).thenReturn(sevenThirtyOne);

            job.execute();
        }

        verifyNoInteractions(pushNotificationService);
    }

    @Test
    @DisplayName("execute_taskAlreadyCompletedToday_doesNotSend")
    void execute_taskAlreadyCompletedToday_doesNotSend() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var area = AreaJpaEntity.builder().id(1L).name("English").user(user).build();
        var task = TaskJpaEntity.builder()
                .id(10L).title("Practice Speaking").area(area)
                .scheduleType(ScheduleType.DAY_OF_WEEK).dayOfWeek(DayOfWeek.MONDAY)
                .reminderTime(SEVEN_THIRTY).orderIndex(0).build();

        var completedLog = DailyLogJpaEntity.builder()
                .task(task).user(user).logDate(MONDAY)
                .completed(true).completedAt(Instant.now()).build();

        when(taskJpaRepository.findByReminderTime(SEVEN_THIRTY)).thenReturn(List.of(task));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, MONDAY)).thenReturn(List.of(completedLog));

        try (MockedStatic<LocalDate> dateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
             MockedStatic<LocalTime> timeMock = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            dateMock.when(() -> LocalDate.now(any(ZoneId.class))).thenReturn(MONDAY);
            timeMock.when(() -> LocalTime.now(any(ZoneId.class))).thenReturn(SEVEN_THIRTY);

            job.execute();
        }

        verifyNoInteractions(pushNotificationService);
    }

    @Test
    @DisplayName("execute_taskDoesNotApplyToday_doesNotSend")
    void execute_taskDoesNotApplyToday_doesNotSend() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var area = AreaJpaEntity.builder().id(1L).name("English").user(user).build();
        // Task is for TUESDAY but today is MONDAY
        var task = TaskJpaEntity.builder()
                .id(10L).title("Practice Speaking").area(area)
                .scheduleType(ScheduleType.DAY_OF_WEEK).dayOfWeek(DayOfWeek.TUESDAY)
                .reminderTime(SEVEN_THIRTY).orderIndex(0).build();

        when(taskJpaRepository.findByReminderTime(SEVEN_THIRTY)).thenReturn(List.of(task));

        try (MockedStatic<LocalDate> dateMock = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
             MockedStatic<LocalTime> timeMock = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            dateMock.when(() -> LocalDate.now(any(ZoneId.class))).thenReturn(MONDAY);
            timeMock.when(() -> LocalTime.now(any(ZoneId.class))).thenReturn(SEVEN_THIRTY);

            job.execute();
        }

        verifyNoInteractions(pushNotificationService);
    }
}

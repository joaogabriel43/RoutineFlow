package com.routineflow.unit.usecase;

import com.routineflow.application.dto.CreateTaskRequest;
import com.routineflow.application.dto.ReorderTasksRequest;
import com.routineflow.application.dto.UpdateTaskRequest;
import com.routineflow.application.usecase.TaskUseCase;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskUseCaseTest {

    @Mock private TaskJpaRepository taskJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;

    private TaskUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long AREA_ID = 10L;
    private static final Long TASK_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new TaskUseCase(taskJpaRepository, areaJpaRepository);
    }

    // ── createTask — DAY_OF_WEEK ─────────────────────────────────────────────

    @Test
    @DisplayName("createTask_dayOfWeek_validRequest_savesAndReturnsResponse")
    void createTask_dayOfWeek_validRequest_savesAndReturnsResponse() {
        var request = new CreateTaskRequest(
                "Re-tell Lecture", "3 re-tells", 30,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null);
        var area = buildArea(AREA_ID, USER_ID);
        var savedTask = buildTask(TASK_ID, area, "Re-tell Lecture", "3 re-tells", 30,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 0);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(AREA_ID)).thenReturn(List.of());
        when(taskJpaRepository.save(any())).thenReturn(savedTask);

        var result = useCase.createTask(USER_ID, AREA_ID, request);

        assertThat(result.id()).isEqualTo(TASK_ID);
        assertThat(result.title()).isEqualTo("Re-tell Lecture");
        assertThat(result.scheduleType()).isEqualTo(ScheduleType.DAY_OF_WEEK);
        assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.dayOfMonth()).isNull();
        assertThat(result.orderIndex()).isZero();
        verify(taskJpaRepository).save(any());
    }

    @Test
    @DisplayName("createTask_dayOfWeek_withoutDayOfWeek_throwsIllegalArgumentException")
    void createTask_dayOfWeek_withoutDayOfWeek_throwsIllegalArgumentException() {
        var request = new CreateTaskRequest(
                "Study", null, null,
                ScheduleType.DAY_OF_WEEK, null, null); // dayOfWeek missing
        var area = buildArea(AREA_ID, USER_ID);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> useCase.createTask(USER_ID, AREA_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfWeek");

        verify(taskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask_areaNotFound_throwsResourceNotFoundException")
    void createTask_areaNotFound_throwsResourceNotFoundException() {
        var request = new CreateTaskRequest(
                "Study", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.TUESDAY, null);
        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createTask(USER_ID, AREA_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(AREA_ID));

        verify(taskJpaRepository, never()).save(any());
    }

    // ── createTask — DAY_OF_MONTH ────────────────────────────────────────────

    @Test
    @DisplayName("createTask_dayOfMonth_validRequest_savesAndReturnsResponse")
    void createTask_dayOfMonth_validRequest_savesAndReturnsResponse() {
        var request = new CreateTaskRequest(
                "Monthly Bill", "Pay rent", null,
                ScheduleType.DAY_OF_MONTH, null, 25);
        var area = buildArea(AREA_ID, USER_ID);
        var savedTask = buildTask(TASK_ID, area, "Monthly Bill", "Pay rent", null,
                ScheduleType.DAY_OF_MONTH, null, 25, 0);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(AREA_ID)).thenReturn(List.of());
        when(taskJpaRepository.save(any())).thenReturn(savedTask);

        var result = useCase.createTask(USER_ID, AREA_ID, request);

        assertThat(result.scheduleType()).isEqualTo(ScheduleType.DAY_OF_MONTH);
        assertThat(result.dayOfWeek()).isNull();
        assertThat(result.dayOfMonth()).isEqualTo(25);
        verify(taskJpaRepository).save(any());
    }

    @Test
    @DisplayName("createTask_dayOfMonth_withoutDayOfMonth_throwsIllegalArgumentException")
    void createTask_dayOfMonth_withoutDayOfMonth_throwsIllegalArgumentException() {
        var request = new CreateTaskRequest(
                "Bill", null, null,
                ScheduleType.DAY_OF_MONTH, null, null); // dayOfMonth missing
        var area = buildArea(AREA_ID, USER_ID);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> useCase.createTask(USER_ID, AREA_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfMonth");

        verify(taskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask_dayOfMonth_dayOfMonth0_throwsIllegalArgumentException")
    void createTask_dayOfMonth_dayOfMonth0_throwsIllegalArgumentException() {
        // Note: @Min(1) bean validation catches 0 at controller layer, but UseCase also validates.
        // We test UseCase directly here — simulate a bypass of bean validation.
        var request = new CreateTaskRequest(
                "Bill", null, null,
                ScheduleType.DAY_OF_MONTH, null, 0);
        var area = buildArea(AREA_ID, USER_ID);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> useCase.createTask(USER_ID, AREA_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfMonth");

        verify(taskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTask_dayOfMonth_dayOfMonth32_throwsIllegalArgumentException")
    void createTask_dayOfMonth_dayOfMonth32_throwsIllegalArgumentException() {
        var request = new CreateTaskRequest(
                "Bill", null, null,
                ScheduleType.DAY_OF_MONTH, null, 32);
        var area = buildArea(AREA_ID, USER_ID);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> useCase.createTask(USER_ID, AREA_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfMonth");

        verify(taskJpaRepository, never()).save(any());
    }

    // ── updateTask ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTask_dayOfWeek_validRequest_updatesAndReturnsResponse")
    void updateTask_dayOfWeek_validRequest_updatesAndReturnsResponse() {
        var request = new UpdateTaskRequest(
                "Listening", "Podcast 30min", 30,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.WEDNESDAY, null);
        var area = buildArea(AREA_ID, USER_ID);
        var task = buildTask(TASK_ID, area, "Old Title", "old desc", 20,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 0);

        when(taskJpaRepository.findByIdAndArea_User_Id(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.updateTask(USER_ID, TASK_ID, request);

        assertThat(result.title()).isEqualTo("Listening");
        assertThat(result.scheduleType()).isEqualTo(ScheduleType.DAY_OF_WEEK);
        assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(result.dayOfMonth()).isNull();
        verify(taskJpaRepository).save(any());
    }

    @Test
    @DisplayName("updateTask_changingScheduleType_fromDayOfWeekToDayOfMonth_updatesCorrectly")
    void updateTask_changingScheduleType_fromDayOfWeekToDayOfMonth_updatesCorrectly() {
        var request = new UpdateTaskRequest(
                "Monthly Review", null, null,
                ScheduleType.DAY_OF_MONTH, null, 25); // changing to DAY_OF_MONTH day=25
        var area = buildArea(AREA_ID, USER_ID);
        var task = buildTask(TASK_ID, area, "Weekly Review", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 0);

        when(taskJpaRepository.findByIdAndArea_User_Id(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.updateTask(USER_ID, TASK_ID, request);

        assertThat(result.scheduleType()).isEqualTo(ScheduleType.DAY_OF_MONTH);
        assertThat(result.dayOfWeek()).isNull();
        assertThat(result.dayOfMonth()).isEqualTo(25);
        verify(taskJpaRepository).save(any());
    }

    @Test
    @DisplayName("updateTask_taskNotFound_throwsResourceNotFoundException")
    void updateTask_taskNotFound_throwsResourceNotFoundException() {
        var request = new UpdateTaskRequest(
                "X", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.FRIDAY, null);
        when(taskJpaRepository.findByIdAndArea_User_Id(TASK_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateTask(USER_ID, TASK_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskJpaRepository, never()).save(any());
    }

    // ── deleteTask ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask_validRequest_callsDeleteById_neverTouchesDailyLog")
    void deleteTask_validRequest_callsDeleteById_neverTouchesDailyLog() {
        var area = buildArea(AREA_ID, USER_ID);
        var task = buildTask(TASK_ID, area, "Study", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 0);

        when(taskJpaRepository.findByIdAndArea_User_Id(TASK_ID, USER_ID)).thenReturn(Optional.of(task));

        useCase.deleteTask(USER_ID, TASK_ID);

        verify(taskJpaRepository).deleteById(TASK_ID);
        verify(taskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTask_taskNotFound_throwsResourceNotFoundException")
    void deleteTask_taskNotFound_throwsResourceNotFoundException() {
        when(taskJpaRepository.findByIdAndArea_User_Id(TASK_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deleteTask(USER_ID, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskJpaRepository, never()).deleteById(any());
    }

    // ── reorderTasks ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("reorderTasks_validRequest_savesAllWithUpdatedOrderIndex")
    void reorderTasks_validRequest_savesAllWithUpdatedOrderIndex() {
        var area = buildArea(AREA_ID, USER_ID);
        var task1 = buildTask(1L, area, "First", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 1);
        var task2 = buildTask(2L, area, "Second", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null, 0);
        var request = new ReorderTasksRequest(List.of(2L, 1L));

        when(taskJpaRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(task2, task1));
        when(taskJpaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(AREA_ID)).thenReturn(List.of(task2, task1));

        useCase.reorderTasks(USER_ID, AREA_ID, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskJpaRepository).saveAll(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.stream().filter(t -> t.getId().equals(2L)).findFirst())
                .get().extracting(TaskJpaEntity::getOrderIndex).isEqualTo(0);
        assertThat(saved.stream().filter(t -> t.getId().equals(1L)).findFirst())
                .get().extracting(TaskJpaEntity::getOrderIndex).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AreaJpaEntity buildArea(Long areaId, Long userId) {
        var user = UserJpaEntity.builder().id(userId).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).name("Test").active(true).build();
        return AreaJpaEntity.builder()
                .id(areaId)
                .user(user)
                .routine(routine)
                .name("Test Area")
                .color("#3B82F6")
                .icon("📚")
                .orderIndex(0)
                .tasks(new ArrayList<>())
                .build();
    }

    private TaskJpaEntity buildTask(Long id, AreaJpaEntity area, String title,
                                    String description, Integer estimatedMinutes,
                                    ScheduleType scheduleType, DayOfWeek dayOfWeek,
                                    Integer dayOfMonth, int orderIndex) {
        return TaskJpaEntity.builder()
                .id(id)
                .area(area)
                .title(title)
                .description(description)
                .estimatedMinutes(estimatedMinutes)
                .scheduleType(scheduleType)
                .dayOfWeek(dayOfWeek)
                .dayOfMonth(dayOfMonth)
                .orderIndex(orderIndex)
                .build();
    }
}

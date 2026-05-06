package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.CheckInUseCase;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInUseCaseTest {

    @Mock private TaskJpaRepository taskJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;

    private CheckInUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long TASK_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 19);

    @BeforeEach
    void setUp() {
        useCase = new CheckInUseCase(taskJpaRepository, dailyLogJpaRepository);
    }

    @Test
    @DisplayName("completeTask_noExistingLog_createsNewLogWithCompletedTrue")
    void completeTask_noExistingLog_createsNewLogWithCompletedTrue() {
        var task = buildTask(TASK_ID, USER_ID);
        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(dailyLogJpaRepository.findByTaskIdAndUserIdAndLogDate(TASK_ID, USER_ID, TODAY))
                .thenReturn(Optional.empty());
        when(dailyLogJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.completeTask(USER_ID, TASK_ID, TODAY);

        assertThat(result.completed()).isTrue();
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.logDate()).isEqualTo(TODAY);
        verify(dailyLogJpaRepository).save(any());
    }

    @Test
    @DisplayName("completeTask_existingLog_updatesLogToCompleted")
    void completeTask_existingLog_updatesLogToCompleted() {
        var task = buildTask(TASK_ID, USER_ID);
        var existingLog = DailyLogJpaEntity.builder()
                .task(task).user(task.getArea().getUser())
                .logDate(TODAY).completed(false).build();

        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(dailyLogJpaRepository.findByTaskIdAndUserIdAndLogDate(TASK_ID, USER_ID, TODAY))
                .thenReturn(Optional.of(existingLog));
        when(dailyLogJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.completeTask(USER_ID, TASK_ID, TODAY);

        assertThat(result.completed()).isTrue();
        assertThat(result.completedAt()).isNotNull();
        // Upsert — não cria novo
        verify(dailyLogJpaRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("uncompleteTask_existingCompletedLog_updatesToFalseAndNullsCompletedAt")
    void uncompleteTask_existingCompletedLog_updatesToFalseAndNullsCompletedAt() {
        var task = buildTask(TASK_ID, USER_ID);
        var existingLog = DailyLogJpaEntity.builder()
                .task(task).user(task.getArea().getUser())
                .logDate(TODAY).completed(true).completedAt(java.time.Instant.now()).build();

        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(dailyLogJpaRepository.findByTaskIdAndUserIdAndLogDate(TASK_ID, USER_ID, TODAY))
                .thenReturn(Optional.of(existingLog));
        when(dailyLogJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.uncompleteTask(USER_ID, TASK_ID, TODAY);

        assertThat(result.completed()).isFalse();
        assertThat(result.completedAt()).isNull();
    }

    @Test
    @DisplayName("completeTask_taskBelongsToAnotherUser_throwsUnauthorizedException")
    void completeTask_taskBelongsToAnotherUser_throwsUnauthorizedException() {
        // Task pertence a OTHER_USER_ID, mas chamada é com USER_ID
        var task = buildTask(TASK_ID, OTHER_USER_ID);
        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> useCase.completeTask(USER_ID, TASK_ID, TODAY))
                .isInstanceOf(UnauthorizedException.class);

        verify(dailyLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeTask_taskNotFound_throwsResourceNotFoundException")
    void completeTask_taskNotFound_throwsResourceNotFoundException() {
        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.completeTask(USER_ID, TASK_ID, TODAY))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("completeTask_pastDate_createsLogWithCorrectLogDate")
    void completeTask_pastDate_createsLogWithCorrectLogDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        var task = buildTask(TASK_ID, USER_ID);
        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(dailyLogJpaRepository.findByTaskIdAndUserIdAndLogDate(TASK_ID, USER_ID, yesterday))
                .thenReturn(Optional.empty());
        when(dailyLogJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.completeTask(USER_ID, TASK_ID, yesterday);

        assertThat(result.logDate()).isEqualTo(yesterday);
        assertThat(result.completed()).isTrue();
        verify(dailyLogJpaRepository).save(any());
    }

    @Test
    @DisplayName("completeTask_futureDate_throwsIllegalArgumentException")
    void completeTask_futureDate_throwsIllegalArgumentException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // validation fires before any repository call — no stubbing needed
        assertThatThrownBy(() -> useCase.completeTask(USER_ID, TASK_ID, tomorrow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");

        verify(taskJpaRepository, never()).findById(any());
        verify(dailyLogJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("uncompleteTask_pastDate_updatesLogWithCorrectDate")
    void uncompleteTask_pastDate_updatesLogWithCorrectDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        var task = buildTask(TASK_ID, USER_ID);
        var existingLog = DailyLogJpaEntity.builder()
                .task(task).user(task.getArea().getUser())
                .logDate(yesterday).completed(true).completedAt(java.time.Instant.now()).build();

        when(taskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(dailyLogJpaRepository.findByTaskIdAndUserIdAndLogDate(TASK_ID, USER_ID, yesterday))
                .thenReturn(Optional.of(existingLog));
        when(dailyLogJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.uncompleteTask(USER_ID, TASK_ID, yesterday);

        assertThat(result.logDate()).isEqualTo(yesterday);
        assertThat(result.completed()).isFalse();
        assertThat(result.completedAt()).isNull();
    }

    private TaskJpaEntity buildTask(Long taskId, Long ownerId) {
        var owner = UserJpaEntity.builder().id(ownerId).build();
        var area = AreaJpaEntity.builder().user(owner).build();
        return TaskJpaEntity.builder()
                .id(taskId)
                .area(area)
                .title("Task")
                .dayOfWeek(DayOfWeek.SUNDAY)
                .orderIndex(0)
                .build();
    }
}

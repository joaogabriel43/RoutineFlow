package com.routineflow.application.usecase;

import com.routineflow.application.dto.BackupData;
import com.routineflow.application.dto.BackupData.*;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.SingleTaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SingleTaskJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportBackupUseCase {

    private final RoutineJpaRepository routineRepository;
    private final SingleTaskJpaRepository singleTaskRepository;
    private final DailyLogJpaRepository dailyLogRepository;
    private final AuthenticatedUserResolver userResolver;

    public ExportBackupUseCase(RoutineJpaRepository routineRepository,
                               SingleTaskJpaRepository singleTaskRepository,
                               DailyLogJpaRepository dailyLogRepository,
                               AuthenticatedUserResolver userResolver) {
        this.routineRepository = routineRepository;
        this.singleTaskRepository = singleTaskRepository;
        this.dailyLogRepository = dailyLogRepository;
        this.userResolver = userResolver;
    }

    @Transactional(readOnly = true)
    public BackupData getFullBackup() {
        Long userId = userResolver.currentUserId();

        List<RoutineJpaEntity> routines = routineRepository.findAllByUserId(userId);
        
        List<SingleTaskJpaEntity> singleTasks = new java.util.ArrayList<>();
        singleTasks.addAll(singleTaskRepository.findPendingByUserId(userId));
        singleTasks.addAll(singleTaskRepository.findArchivedByUserId(userId));

        // Fetch logs for all tasks belonging to user's routines
        // For simplicity and to avoid complex joins, we can query them directly if we have a custom method.
        // Or we can just get all logs if we add a user_id to logs, but logs are linked via tasks.
        // The repository has findForExport which gets CheckInExportRow, but we need the raw logs.
        // Let's create a query in DailyLogJpaRepository for this.

        List<RoutineBackup> routineBackups = routines.stream().map(r -> new RoutineBackup(
                r.getId(),
                r.getName(),
                r.isActive(),
                r.getAreas().stream().map(a -> new AreaBackup(
                        a.getId(),
                        a.getName(),
                        a.getColor(),
                        a.getIcon(),
                        a.getResetFrequency().name(),
                        a.getTasks().stream().map(t -> new TaskBackup(
                                t.getId(),
                                t.getTitle(),
                                t.getDescription(),
                                t.getEstimatedMinutes(),
                                t.getScheduleType().name(),
                                t.getDayOfWeek() != null ? t.getDayOfWeek().name() : null,
                                t.getDayOfMonth()
                        )).collect(Collectors.toList())
                )).collect(Collectors.toList())
        )).collect(Collectors.toList());

        List<SingleTaskBackup> singleTaskBackups = singleTasks.stream().map(st -> new SingleTaskBackup(
                st.getId(),
                st.getTitle(),
                st.getDescription(),
                st.getDueDate() != null ? st.getDueDate().toString() : null,
                st.isCompleted(),
                st.getCompletedAt() != null ? st.getCompletedAt().toString() : null
        )).collect(Collectors.toList());

        List<DailyLogBackup> dailyLogBackups = dailyLogRepository.findByUserId(userId).stream()
                .map(dl -> new DailyLogBackup(
                        dl.getId(),
                        dl.getTask().getId(),
                        dl.getLogDate().toString(),
                        dl.isCompleted(),
                        dl.getCompletedAt() != null ? dl.getCompletedAt().toString() : null
                )).collect(Collectors.toList());

        return new BackupData(
                Instant.now().toString(),
                "1.0",
                routineBackups,
                singleTaskBackups,
                dailyLogBackups
        );
    }
}

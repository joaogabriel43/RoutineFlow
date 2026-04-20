package com.routineflow.infrastructure.scheduler;

import com.routineflow.domain.service.StreakCalculationService;
import com.routineflow.infrastructure.persistence.entity.SchedulerRunJpaEntity;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SchedulerRunJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DailyResetJob {

    private static final Logger log = LoggerFactory.getLogger(DailyResetJob.class);
    private static final String JOB_NAME = "DailyResetJob";

    private final StreakCalculationService streakCalculationService;
    private final RoutineJpaRepository routineJpaRepository;
    private final SchedulerRunJpaRepository schedulerRunJpaRepository;

    public DailyResetJob(
            StreakCalculationService streakCalculationService,
            RoutineJpaRepository routineJpaRepository,
            SchedulerRunJpaRepository schedulerRunJpaRepository
    ) {
        this.streakCalculationService = streakCalculationService;
        this.routineJpaRepository = routineJpaRepository;
        this.schedulerRunJpaRepository = schedulerRunJpaRepository;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    public void execute() {
        Instant startedAt = Instant.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[{}] Starting — evaluating streaks for date={}", JOB_NAME, yesterday);

        var run = SchedulerRunJpaEntity.builder()
                .jobName(JOB_NAME)
                .startedAt(startedAt)
                .status("RUNNING")
                .build();
        run = schedulerRunJpaRepository.save(run);

        var activeRoutines = routineJpaRepository.findAllActiveRoutines();
        int processed = 0;
        List<String> errors = new ArrayList<>();

        for (var routine : activeRoutines) {
            Long userId = routine.getUser().getId();
            try {
                streakCalculationService.calculate(userId, yesterday);
                processed++;
            } catch (Exception e) {
                // Falha de um usuário não para os demais
                log.error("[{}] Error processing userId={}: {}", JOB_NAME, userId, e.getMessage());
                errors.add("userId=" + userId + ": " + e.getMessage());
            }
        }

        String status = errors.isEmpty() ? "SUCCESS"
                : (processed > 0 ? "PARTIAL_FAILURE" : "FAILURE");
        run.setFinishedAt(Instant.now());
        run.setUsersProcessed(processed);
        run.setStatus(status);
        run.setErrorDetails(errors.isEmpty() ? null : String.join("\n", errors));
        schedulerRunJpaRepository.save(run);

        log.info("[{}] Finished — processed={}, status={}", JOB_NAME, processed, status);
    }
}

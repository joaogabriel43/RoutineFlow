package com.routineflow.unit.usecase;

import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedRoutine;
import com.routineflow.application.dto.ParsedTask;
import com.routineflow.application.usecase.ImportRoutineUseCase;
import com.routineflow.domain.model.ImportMode;
import com.routineflow.infrastructure.parser.RoutineFileParser;
import com.routineflow.infrastructure.parser.RoutineParseException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportRoutineUseCaseTest {

    @Mock private RoutineFileParser yamlParser;
    @Mock private RoutineFileParser txtParser;
    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private TaskJpaRepository taskJpaRepository;
    @Mock private UserJpaRepository userJpaRepository;

    private ImportRoutineUseCase useCase;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        when(yamlParser.supports("yaml")).thenReturn(true);
        when(yamlParser.supports("yml")).thenReturn(true);
        when(yamlParser.supports("txt")).thenReturn(false);
        when(txtParser.supports("txt")).thenReturn(true);
        when(txtParser.supports("yaml")).thenReturn(false);
        when(txtParser.supports("yml")).thenReturn(false);

        useCase = new ImportRoutineUseCase(
                List.of(yamlParser, txtParser),
                routineJpaRepository,
                areaJpaRepository,
                taskJpaRepository,
                userJpaRepository
        );
    }

    // ── REPLACE — existing tests (updated to new signature) ──────────────────

    @Test
    @DisplayName("execute_validYaml_deactivatesPreviousAndPersistsNew")
    void execute_validYaml_deactivatesPreviousAndPersistsNew() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).email("u@test.com").name("U").build();
        var parsedRoutine = buildParsedRoutine();
        var savedRoutine = RoutineJpaEntity.builder().id(10L).name("Rotina").build();

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsedRoutine);
        when(routineJpaRepository.save(any())).thenReturn(savedRoutine);
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var area = (AreaJpaEntity) i.getArgument(0);
            area.setId(99L);
            return area;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.REPLACE);

        verify(routineJpaRepository).deactivateAllByUserId(USER_ID);
        verify(routineJpaRepository).save(any());
        assertThat(response.name()).isEqualTo("Rotina");
        assertThat(response.totalAreas()).isEqualTo(1);
        assertThat(response.totalTasks()).isEqualTo(1);
        assertThat(response.mode()).isEqualTo(ImportMode.REPLACE);
    }

    @Test
    @DisplayName("execute_noActiveRoutinePreviously_importNormally")
    void execute_noActiveRoutinePreviously_importNormally() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).email("u@test.com").name("U").build();
        var parsedRoutine = buildParsedRoutine();
        var savedRoutine = RoutineJpaEntity.builder().id(10L).name("Rotina").build();

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsedRoutine);
        when(routineJpaRepository.save(any())).thenReturn(savedRoutine);
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var area = (AreaJpaEntity) i.getArgument(0);
            area.setId(99L);
            return area;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.REPLACE);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("execute_parserThrows_nothingIsPersisted")
    void execute_parserThrows_nothingIsPersisted() {
        when(userJpaRepository.findById(USER_ID)).thenReturn(
                Optional.of(UserJpaEntity.builder().id(USER_ID).build())
        );
        when(yamlParser.parse(any())).thenThrow(new RoutineParseException("invalid yaml"));

        assertThatThrownBy(() ->
                useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.REPLACE)
        ).isInstanceOf(RoutineParseException.class);

        verify(routineJpaRepository, never()).save(any());
        verify(areaJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute_unsupportedExtension_throwsIllegalArgument")
    void execute_unsupportedExtension_throwsIllegalArgument() {
        when(userJpaRepository.findById(USER_ID)).thenReturn(
                Optional.of(UserJpaEntity.builder().id(USER_ID).build())
        );

        assertThatThrownBy(() ->
                useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "pdf", ImportMode.REPLACE)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pdf");
    }

    // ── REPLACE — new tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("importWithReplace_returnsCorrectMode")
    void importWithReplace_returnsCorrectMode() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var savedRoutine = RoutineJpaEntity.builder().id(5L).name("R").build();

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(buildParsedRoutine());
        when(routineJpaRepository.save(any())).thenReturn(savedRoutine);
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var a = (AreaJpaEntity) i.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.REPLACE);

        assertThat(response.mode()).isEqualTo(ImportMode.REPLACE);
        assertThat(response.areasCreated()).isEqualTo(1);
        assertThat(response.tasksCreated()).isEqualTo(1);
        assertThat(response.tasksSkipped()).isZero();
        verify(routineJpaRepository).deactivateAllByUserId(USER_ID);
    }

    // ── MERGE — new area added ─────────────────────────────────────────────────

    @Test
    @DisplayName("importWithMerge_noActiveRoutine_createsNewLikeReplace")
    void importWithMerge_noActiveRoutine_createsNewLikeReplace() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var savedRoutine = RoutineJpaEntity.builder().id(5L).name("Rotina").build();

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(buildParsedRoutine());
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(routineJpaRepository.save(any())).thenReturn(savedRoutine);
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var a = (AreaJpaEntity) i.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.MERGE);

        // No active routine → behaves like REPLACE (creates new routine)
        verify(routineJpaRepository).deactivateAllByUserId(USER_ID);
        verify(routineJpaRepository).save(any());
        assertThat(response.mode()).isEqualTo(ImportMode.MERGE);
        assertThat(response.areasCreated()).isEqualTo(1);
        assertThat(response.tasksCreated()).isEqualTo(1);
    }

    @Test
    @DisplayName("importWithMerge_existingRoutine_addsNewArea")
    void importWithMerge_existingRoutine_addsNewArea() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var activeRoutine = buildActiveRoutine(userEntity);

        // Active routine has area "Existing Area" — parsed file has "New Area" → should be created
        var parsedTask = new ParsedTask("New Task", null, 30, 0);
        var parsedArea = new ParsedArea("New Area", "#FF0000", "🎯", Map.of(DayOfWeek.MONDAY, List.of(parsedTask)));
        var parsedRoutine = new ParsedRoutine("Rotina", List.of(parsedArea));

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsedRoutine);
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(activeRoutine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(activeRoutine.getId()))
                .thenReturn(List.of(buildExistingArea(userEntity, activeRoutine, "Existing Area")));
        when(areaJpaRepository.findByRoutineId(activeRoutine.getId()))
                .thenReturn(List.of(buildExistingArea(userEntity, activeRoutine, "Existing Area")));
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var a = (AreaJpaEntity) i.getArgument(0);
            a.setId(200L);
            return a;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.MERGE);

        // No deactivation — merge keeps active routine
        verify(routineJpaRepository, never()).deactivateAllByUserId(USER_ID);
        verify(routineJpaRepository, never()).save(any()); // no new routine created
        assertThat(response.mode()).isEqualTo(ImportMode.MERGE);
        assertThat(response.areasCreated()).isEqualTo(1);
        assertThat(response.areasMerged()).isZero();
        assertThat(response.tasksCreated()).isEqualTo(1);
        assertThat(response.tasksSkipped()).isZero();
    }

    @Test
    @DisplayName("importWithMerge_existingRoutine_existingArea_addsNewTasks")
    void importWithMerge_existingRoutine_existingArea_addsNewTasks() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var activeRoutine = buildActiveRoutine(userEntity);

        // Active routine area "Área 1" already has "Existing Task" on MONDAY
        // Parsed file area "Área 1" has "New Task" on TUESDAY → should be added
        var existingArea = buildExistingAreaWithTask(userEntity, activeRoutine, "Área 1",
                "Existing Task", DayOfWeek.MONDAY);

        var parsedTask = new ParsedTask("New Task", null, null, 0);
        var parsedArea = new ParsedArea("Área 1", "#FF0000", "🎯",
                Map.of(DayOfWeek.TUESDAY, List.of(parsedTask)));
        var parsedRoutine = new ParsedRoutine("Rotina", List.of(parsedArea));

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsedRoutine);
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(activeRoutine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(activeRoutine.getId()))
                .thenReturn(List.of(existingArea));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(existingArea.getId()))
                .thenReturn(existingArea.getTasks());
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.MERGE);

        assertThat(response.mode()).isEqualTo(ImportMode.MERGE);
        assertThat(response.areasCreated()).isZero();
        assertThat(response.areasMerged()).isEqualTo(1);
        assertThat(response.tasksCreated()).isEqualTo(1); // "New Task" added
        assertThat(response.tasksSkipped()).isZero();
        verify(routineJpaRepository, never()).deactivateAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("importWithMerge_existingRoutine_existingArea_duplicateTask_ignored")
    void importWithMerge_existingRoutine_existingArea_duplicateTask_ignored() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var activeRoutine = buildActiveRoutine(userEntity);

        // "Área 1" already has "Task 1" on MONDAY — parsed file also has "Task 1" on MONDAY
        var existingArea = buildExistingAreaWithTask(userEntity, activeRoutine, "Área 1",
                "Task 1", DayOfWeek.MONDAY);

        var parsedTask = new ParsedTask("Task 1", "different desc", 60, 0); // same title, same day
        var parsedArea = new ParsedArea("Área 1", "#000", "🎯",
                Map.of(DayOfWeek.MONDAY, List.of(parsedTask)));
        var parsedRoutine = new ParsedRoutine("Rotina", List.of(parsedArea));

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsedRoutine);
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(activeRoutine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(activeRoutine.getId()))
                .thenReturn(List.of(existingArea));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(existingArea.getId()))
                .thenReturn(existingArea.getTasks());

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.MERGE);

        assertThat(response.mode()).isEqualTo(ImportMode.MERGE);
        assertThat(response.areasMerged()).isEqualTo(1);
        assertThat(response.tasksCreated()).isZero();
        assertThat(response.tasksSkipped()).isEqualTo(1); // exact duplicate ignored
        verify(taskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("importWithMerge_existingRoutine_multipleAreas_mixedResult")
    void importWithMerge_existingRoutine_multipleAreas_mixedResult() {
        var userEntity = UserJpaEntity.builder().id(USER_ID).build();
        var activeRoutine = buildActiveRoutine(userEntity);

        // Existing: "Área Existente" with "Task A" on MONDAY
        var existingArea = buildExistingAreaWithTask(userEntity, activeRoutine,
                "Área Existente", "Task A", DayOfWeek.MONDAY);

        // Parsed: "Área Existente" with "Task A" (MONDAY=dup) + "Área Nova" with "Task B"
        var dupTask    = new ParsedTask("Task A", null, null, 0);
        var newTask    = new ParsedTask("Task B", null, null, 0);
        var existArea  = new ParsedArea("Área Existente", "#000", "🎯",
                Map.of(DayOfWeek.MONDAY, List.of(dupTask)));
        var brandNew   = new ParsedArea("Área Nova", "#FFF", "🚀",
                Map.of(DayOfWeek.WEDNESDAY, List.of(newTask)));
        var parsed     = new ParsedRoutine("Rotina", List.of(existArea, brandNew));

        when(userJpaRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(yamlParser.parse(any())).thenReturn(parsed);
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(activeRoutine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(activeRoutine.getId()))
                .thenReturn(List.of(existingArea));
        when(areaJpaRepository.findByRoutineId(activeRoutine.getId()))
                .thenReturn(List.of(existingArea));
        when(taskJpaRepository.findByAreaIdOrderByOrderIndex(existingArea.getId()))
                .thenReturn(existingArea.getTasks());
        when(areaJpaRepository.save(any())).thenAnswer(i -> {
            var a = (AreaJpaEntity) i.getArgument(0);
            a.setId(300L);
            return a;
        });
        when(taskJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml", ImportMode.MERGE);

        assertThat(response.areasCreated()).isEqualTo(1);  // "Área Nova"
        assertThat(response.areasMerged()).isEqualTo(1);   // "Área Existente"
        assertThat(response.tasksCreated()).isEqualTo(1);  // "Task B"
        assertThat(response.tasksSkipped()).isEqualTo(1);  // "Task A" duplicate
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ParsedRoutine buildParsedRoutine() {
        var task = new ParsedTask("Task 1", "desc", 30, 0);
        var area = new ParsedArea("Área 1", "#000", "🎯", Map.of(DayOfWeek.MONDAY, List.of(task)));
        return new ParsedRoutine("Rotina", List.of(area));
    }

    private RoutineJpaEntity buildActiveRoutine(UserJpaEntity user) {
        return RoutineJpaEntity.builder()
                .id(42L)
                .user(user)
                .name("Rotina Ativa")
                .active(true)
                .build();
    }

    private AreaJpaEntity buildExistingArea(UserJpaEntity user, RoutineJpaEntity routine, String name) {
        return AreaJpaEntity.builder()
                .id(10L)
                .user(user)
                .routine(routine)
                .name(name)
                .color("#000")
                .icon("📚")
                .orderIndex(0)
                .tasks(new ArrayList<>())
                .build();
    }

    private AreaJpaEntity buildExistingAreaWithTask(UserJpaEntity user, RoutineJpaEntity routine,
                                                     String areaName, String taskTitle,
                                                     DayOfWeek day) {
        var area = buildExistingArea(user, routine, areaName);
        var task = TaskJpaEntity.builder()
                .id(100L)
                .area(area)
                .title(taskTitle)
                .dayOfWeek(day)
                .orderIndex(0)
                .build();
        area.getTasks().add(task);
        return area;
    }
}

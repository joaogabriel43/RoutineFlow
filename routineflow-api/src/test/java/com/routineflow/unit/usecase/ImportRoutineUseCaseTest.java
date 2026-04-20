package com.routineflow.unit.usecase;

import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedRoutine;
import com.routineflow.application.dto.ParsedTask;
import com.routineflow.application.usecase.ImportRoutineUseCase;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml");

        verify(routineJpaRepository).deactivateAllByUserId(USER_ID);
        verify(routineJpaRepository).save(any());
        assertThat(response.name()).isEqualTo("Rotina");
        assertThat(response.totalAreas()).isEqualTo(1);
        assertThat(response.totalTasks()).isEqualTo(1);
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

        // Não lança exceção mesmo sem rotina anterior
        var response = useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml");

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
                useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "yaml")
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
                useCase.execute(USER_ID, new ByteArrayInputStream(new byte[0]), "pdf")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pdf");
    }

    private ParsedRoutine buildParsedRoutine() {
        var task = new ParsedTask("Task 1", "desc", 30, 0);
        var area = new ParsedArea("Área 1", "#000", "🎯", Map.of(DayOfWeek.MONDAY, List.of(task)));
        return new ParsedRoutine("Rotina", List.of(area));
    }
}

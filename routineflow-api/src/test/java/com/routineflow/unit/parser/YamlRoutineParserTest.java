package com.routineflow.unit.parser;

import com.routineflow.infrastructure.parser.RoutineParseException;
import com.routineflow.infrastructure.parser.YamlRoutineParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlRoutineParserTest {

    private YamlRoutineParser parser;

    @BeforeEach
    void setUp() {
        parser = new YamlRoutineParser();
    }

    @Test
    @DisplayName("supports_yamlExtension_returnsTrue")
    void supports_yamlExtension_returnsTrue() {
        assertThat(parser.supports("yaml")).isTrue();
        assertThat(parser.supports("yml")).isTrue();
    }

    @Test
    @DisplayName("supports_txtExtension_returnsFalse")
    void supports_txtExtension_returnsFalse() {
        assertThat(parser.supports("txt")).isFalse();
        assertThat(parser.supports("json")).isFalse();
    }

    @Test
    @DisplayName("parse_validYaml_returnsCorrectParsedRoutine")
    void parse_validYaml_returnsCorrectParsedRoutine() throws Exception {
        InputStream yaml = loadFixture("fixtures/valid_routine.yaml");

        var result = parser.parse(yaml);

        assertThat(result.name()).isEqualTo("Minha Rotina 2026");
        assertThat(result.areas()).hasSize(2);

        var english = result.areas().get(0);
        assertThat(english.name()).isEqualTo("Inglês/PTE");
        assertThat(english.color()).isEqualTo("#3B82F6");
        assertThat(english.icon()).isEqualTo("📚");
        assertThat(english.schedule()).containsKey(DayOfWeek.MONDAY);
        assertThat(english.schedule().get(DayOfWeek.MONDAY)).hasSize(2);
        assertThat(english.schedule().get(DayOfWeek.MONDAY).get(0).title()).isEqualTo("Re-tell Lecture");
        assertThat(english.schedule().get(DayOfWeek.MONDAY).get(0).estimatedMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("parse_yamlMissingRoutineName_throwsRoutineParseException")
    void parse_yamlMissingRoutineName_throwsRoutineParseException() {
        String yaml = """
                routine:
                  areas:
                    - name: "Inglês"
                      color: "#000000"
                      icon: "📚"
                """;

        assertThatThrownBy(() -> parser.parse(toStream(yaml)))
                .isInstanceOf(RoutineParseException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("parse_yamlAreaWithNoTasks_areaIncludedWithEmptySchedule")
    void parse_yamlAreaWithNoTasks_areaIncludedWithEmptySchedule() throws Exception {
        String yaml = """
                routine:
                  name: "Rotina Simples"
                  areas:
                    - name: "Área Vazia"
                      color: "#000000"
                      icon: "🎯"
                """;

        var result = parser.parse(toStream(yaml));

        assertThat(result.areas()).hasSize(1);
        assertThat(result.areas().get(0).schedule()).isEmpty();
    }

    @Test
    @DisplayName("parse_yamlWithNoAreas_throwsRoutineParseException")
    void parse_yamlWithNoAreas_throwsRoutineParseException() {
        String yaml = """
                routine:
                  name: "Rotina Sem Áreas"
                """;

        assertThatThrownBy(() -> parser.parse(toStream(yaml)))
                .isInstanceOf(RoutineParseException.class)
                .hasMessageContaining("area");
    }

    private InputStream loadFixture(String path) {
        return Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path),
                "Fixture not found: " + path
        );
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}

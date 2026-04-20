package com.routineflow.unit.parser;

import com.routineflow.infrastructure.parser.RoutineParseException;
import com.routineflow.infrastructure.parser.TxtRoutineParser;
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

class TxtRoutineParserTest {

    private TxtRoutineParser parser;

    @BeforeEach
    void setUp() {
        parser = new TxtRoutineParser();
    }

    @Test
    @DisplayName("supports_txtExtension_returnsTrue")
    void supports_txtExtension_returnsTrue() {
        assertThat(parser.supports("txt")).isTrue();
    }

    @Test
    @DisplayName("supports_yamlExtension_returnsFalse")
    void supports_yamlExtension_returnsFalse() {
        assertThat(parser.supports("yaml")).isFalse();
    }

    @Test
    @DisplayName("parse_validTxt_returnsCorrectParsedRoutine")
    void parse_validTxt_returnsCorrectParsedRoutine() throws Exception {
        InputStream txt = loadFixture("fixtures/valid_routine.txt");

        var result = parser.parse(txt);

        assertThat(result.name()).isEqualTo("Minha Rotina TXT");
        assertThat(result.areas()).hasSize(2);

        var english = result.areas().get(0);
        assertThat(english.name()).isEqualTo("Inglês/PTE");
        assertThat(english.color()).isEqualTo("#3B82F6");
        assertThat(english.schedule().get(DayOfWeek.MONDAY)).hasSize(2);
        assertThat(english.schedule().get(DayOfWeek.MONDAY).get(0).title()).isEqualTo("Re-tell Lecture");
        assertThat(english.schedule().get(DayOfWeek.MONDAY).get(0).estimatedMinutes()).isEqualTo(30);

        var programming = result.areas().get(1);
        assertThat(programming.name()).isEqualTo("Programação");
        assertThat(programming.schedule().get(DayOfWeek.TUESDAY)).hasSize(1);
    }

    @Test
    @DisplayName("parse_missingRoutineLine_throwsRoutineParseException")
    void parse_missingRoutineLine_throwsRoutineParseException() {
        String txt = "AREA: Inglês | color=#000 | icon=📚\nMONDAY: Task | desc=desc | min=30\n";

        assertThatThrownBy(() -> parser.parse(toStream(txt)))
                .isInstanceOf(RoutineParseException.class)
                .hasMessageContaining("ROUTINE");
    }

    @Test
    @DisplayName("parse_malformedTaskLine_throwsRoutineParseExceptionWithDetail")
    void parse_malformedTaskLine_throwsRoutineParseExceptionWithDetail() {
        // Linha de task com título vazio após o token do dia
        String txt = "ROUTINE: Minha Rotina\nAREA: Inglês | color=#000 | icon=📚\nMONDAY: \n";

        assertThatThrownBy(() -> parser.parse(toStream(txt)))
                .isInstanceOf(RoutineParseException.class)
                .hasMessageContaining("MONDAY");
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

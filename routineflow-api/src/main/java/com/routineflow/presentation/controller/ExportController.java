package com.routineflow.presentation.controller;

import com.routineflow.application.dto.CheckInExportRow;
import com.routineflow.application.usecase.ExportUseCase;
import com.routineflow.infrastructure.config.AppTimeZone;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "Export", description = "CSV export of check-in history")
@RestController
@RequestMapping("/export")
public class ExportController {

    private static final String CSV_HEADER =
            "Data,Dia da Semana,Área,Tarefa,Concluído,Horário de Conclusão";

    private static final Map<DayOfWeek, String> DAY_LABELS = Map.of(
            DayOfWeek.MONDAY,    "Segunda",
            DayOfWeek.TUESDAY,   "Terça",
            DayOfWeek.WEDNESDAY, "Quarta",
            DayOfWeek.THURSDAY,  "Quinta",
            DayOfWeek.FRIDAY,    "Sexta",
            DayOfWeek.SATURDAY,  "Sábado",
            DayOfWeek.SUNDAY,    "Domingo"
    );

    private final ExportUseCase exportUseCase;
    private final AuthenticatedUserResolver userResolver;

    public ExportController(ExportUseCase exportUseCase, AuthenticatedUserResolver userResolver) {
        this.exportUseCase = exportUseCase;
        this.userResolver = userResolver;
    }

    @Operation(summary = "Export check-ins as CSV (UTF-8 BOM, max 365 days, default 90)")
    @GetMapping(value = "/checkins", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportCheckIns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = userResolver.currentUserId();
        List<CheckInExportRow> rows = exportUseCase.getCheckInsForExport(userId, from, to);

        String filename = "routineflow-export-" + LocalDate.now(AppTimeZone.ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        StreamingResponseBody body = outputStream -> {
            // UTF-8 BOM — required for Excel on Windows to recognise accented characters
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            writer.println(CSV_HEADER);

            for (CheckInExportRow row : rows) {
                writer.println(buildCsvLine(row));
            }

            writer.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(body);
    }

    private String buildCsvLine(CheckInExportRow row) {
        String date        = row.logDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dayLabel    = DAY_LABELS.getOrDefault(row.dayOfWeek(), row.dayOfWeek().name());
        String area        = escapeCsv(row.areaName());
        String task        = escapeCsv(row.taskTitle());
        String completed   = row.completed() ? "Sim" : "Não";
        String completedAt = row.completedAt() != null ? row.completedAt().toString() : "";

        return String.join(",", date, dayLabel, area, task, completed, completedAt);
    }

    /**
     * Wraps a CSV field in double-quotes if it contains a comma, double-quote, or newline,
     * and escapes any embedded double-quotes by doubling them (RFC 4180).
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

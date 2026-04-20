package com.routineflow.infrastructure.parser;

import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedRoutine;
import com.routineflow.application.dto.ParsedTask;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TxtRoutineParser implements RoutineFileParser {

    private static final Set<String> DAY_TOKENS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    @Override
    public boolean supports(String fileExtension) {
        return "txt".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedRoutine parse(InputStream inputStream) throws RoutineParseException {
        List<String> lines;
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            lines = reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isBlank())
                    .toList();
        } catch (Exception e) {
            throw new RoutineParseException("Failed to read TXT file: " + e.getMessage(), e);
        }

        if (lines.isEmpty() || !lines.get(0).startsWith("ROUTINE:")) {
            throw new RoutineParseException("First line must start with 'ROUTINE:' — found: "
                    + (lines.isEmpty() ? "<empty>" : lines.get(0)));
        }

        String routineName = lines.get(0).substring("ROUTINE:".length()).trim();

        List<ParsedArea> areas = new ArrayList<>();
        ParsedArea currentArea = null;
        Map<DayOfWeek, List<ParsedTask>> currentSchedule = null;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.startsWith("AREA:")) {
                if (currentArea != null) {
                    areas.add(finalizeArea(currentArea, currentSchedule));
                }
                currentArea = parseAreaLine(line);
                currentSchedule = new LinkedHashMap<>();

            } else if (isDayLine(line)) {
                if (currentArea == null) {
                    throw new RoutineParseException("Task line found before any AREA definition: " + line);
                }
                String dayToken = line.substring(0, line.indexOf(':'));
                DayOfWeek day = DayOfWeek.valueOf(dayToken);
                ParsedTask task = parseTaskLine(line, dayToken, currentSchedule.getOrDefault(day, new ArrayList<>()).size());
                currentSchedule.computeIfAbsent(day, k -> new ArrayList<>()).add(task);

            } else {
                throw new RoutineParseException("Unrecognized line format at line " + (i + 1) + ": " + line);
            }
        }

        if (currentArea != null) {
            areas.add(finalizeArea(currentArea, currentSchedule));
        }

        return new ParsedRoutine(routineName, areas);
    }

    private boolean isDayLine(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) return false;
        return DAY_TOKENS.contains(line.substring(0, colon).toUpperCase());
    }

    private ParsedArea parseAreaLine(String line) {
        // Format: AREA: Name | color=#HEX | icon=EMOJI
        String content = line.substring("AREA:".length()).trim();
        String[] parts = content.split("\\|");
        String name = parts[0].trim();
        String color = "#6B7280";
        String icon = "📋";
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("color=")) color = part.substring("color=".length()).trim();
            if (part.startsWith("icon=")) icon = part.substring("icon=".length()).trim();
        }
        // Placeholder — schedule preenchido depois
        return new ParsedArea(name, color, icon, new LinkedHashMap<>());
    }

    private ParsedTask parseTaskLine(String line, String dayToken, int orderIndex) {
        // Format: MONDAY: Title | desc=Description | min=30
        String content = line.substring(dayToken.length() + 1).trim();
        String[] parts = content.split("\\|");
        if (parts.length < 1 || parts[0].isBlank()) {
            throw new RoutineParseException("Task line for " + dayToken + " must have a title: " + line);
        }
        String title = parts[0].trim();
        String description = "";
        Integer minutes = null;
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("desc=")) description = part.substring("desc=".length()).trim();
            if (part.startsWith("min=")) {
                try {
                    minutes = Integer.parseInt(part.substring("min=".length()).trim());
                } catch (NumberFormatException e) {
                    throw new RoutineParseException("Invalid minutes value in line: " + line);
                }
            }
        }
        return new ParsedTask(title, description, minutes, orderIndex);
    }

    private ParsedArea finalizeArea(ParsedArea area, Map<DayOfWeek, List<ParsedTask>> schedule) {
        return new ParsedArea(area.name(), area.color(), area.icon(), schedule);
    }
}

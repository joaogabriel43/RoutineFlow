package com.routineflow.infrastructure.parser;

import com.routineflow.application.dto.ParsedArea;
import com.routineflow.application.dto.ParsedRoutine;
import com.routineflow.application.dto.ParsedTask;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class YamlRoutineParser implements RoutineFileParser {

    @Override
    public boolean supports(String fileExtension) {
        return "yaml".equalsIgnoreCase(fileExtension) || "yml".equalsIgnoreCase(fileExtension);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ParsedRoutine parse(InputStream inputStream) throws RoutineParseException {
        Map<String, Object> root;
        try {
            root = new Yaml().load(inputStream);
        } catch (Exception e) {
            throw new RoutineParseException("Failed to parse YAML: " + e.getMessage(), e);
        }

        if (root == null || !root.containsKey("routine")) {
            throw new RoutineParseException("Missing root key 'routine'");
        }

        var routine = (Map<String, Object>) root.get("routine");

        String name = (String) routine.get("name");
        if (name == null || name.isBlank()) {
            throw new RoutineParseException("Field 'routine.name' is required and cannot be blank");
        }

        List<Map<String, Object>> rawAreas = (List<Map<String, Object>>) routine.get("areas");
        if (rawAreas == null || rawAreas.isEmpty()) {
            throw new RoutineParseException("At least one area is required under 'routine.areas'");
        }

        List<ParsedArea> areas = new ArrayList<>();
        for (var rawArea : rawAreas) {
            areas.add(parseArea(rawArea));
        }

        return new ParsedRoutine(name, areas);
    }

    @SuppressWarnings("unchecked")
    private ParsedArea parseArea(Map<String, Object> rawArea) {
        String areaName = (String) rawArea.get("name");
        String color = (String) rawArea.getOrDefault("color", "#6B7280");
        String icon = (String) rawArea.getOrDefault("icon", "📋");

        Map<DayOfWeek, List<ParsedTask>> schedule = new LinkedHashMap<>();

        var rawSchedule = (Map<String, List<Map<String, Object>>>) rawArea.get("schedule");
        if (rawSchedule != null) {
            for (var entry : rawSchedule.entrySet()) {
                DayOfWeek day = DayOfWeek.valueOf(entry.getKey().toUpperCase());
                List<ParsedTask> tasks = new ArrayList<>();
                int order = 0;
                for (var rawTask : entry.getValue()) {
                    tasks.add(parseTask(rawTask, order++));
                }
                schedule.put(day, tasks);
            }
        }

        return new ParsedArea(areaName, color, icon, schedule);
    }

    private ParsedTask parseTask(Map<String, Object> rawTask, int orderIndex) {
        String title = (String) rawTask.get("title");
        String description = (String) rawTask.getOrDefault("description", "");
        Integer minutes = (Integer) rawTask.get("estimatedMinutes");
        return new ParsedTask(title, description, minutes, orderIndex);
    }
}

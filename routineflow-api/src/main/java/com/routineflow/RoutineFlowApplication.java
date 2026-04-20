package com.routineflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoutineFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutineFlowApplication.class, args);
    }
}

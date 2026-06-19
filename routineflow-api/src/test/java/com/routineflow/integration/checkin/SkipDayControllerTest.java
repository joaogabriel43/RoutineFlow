package com.routineflow.integration.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.SkipDayRequest;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.SkipDayJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SkipDayJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.security.JwtService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SkipDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RoutineJpaRepository routineJpaRepository;

    @Autowired
    private AreaJpaRepository areaJpaRepository;

    @Autowired
    private SkipDayJpaRepository skipDayJpaRepository;

    @Autowired
    private JwtService jwtService;

    private String token;
    private Long areaId;
    private UserJpaEntity userEntity;

    @BeforeEach
    void setUp() {
        userEntity = userJpaRepository.save(
                UserJpaEntity.builder()
                        .email("skip@example.com")
                        .passwordHash("hashed_password")
                        .name("Skip User")
                        .build()
        );

        UserDetails userDetails = new User(userEntity.getEmail(), userEntity.getPasswordHash(), List.of());
        token = "Bearer " + jwtService.generateToken(userDetails);

        RoutineJpaEntity routine = routineJpaRepository.save(
                RoutineJpaEntity.builder()
                        .user(userEntity)
                        .name("Morning Routine")
                        .active(true)
                        .build()
        );

        AreaJpaEntity area = areaJpaRepository.save(
                AreaJpaEntity.builder()
                        .user(userEntity)
                        .routine(routine)
                        .name("Health")
                        .color("#FF0000")
                        .icon("heart")
                        .build()
        );
        areaId = area.getId();
    }

    @AfterEach
    void tearDown() {
        skipDayJpaRepository.deleteAll();
        areaJpaRepository.deleteAll();
        routineJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void skipDay_success() throws Exception {
        LocalDate date = LocalDate.now();
        SkipDayRequest request = new SkipDayRequest(date, "trip");

        mockMvc.perform(post("/api/areas/{areaId}/skip-days", areaId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason", is("trip")))
                .andExpect(jsonPath("$.skipDate", is(date.toString())));

        List<SkipDayJpaEntity> skipDays = skipDayJpaRepository.findAll();
        assertEquals(1, skipDays.size());
    }

    @Test
    void skipDay_failsIfOverLimit() throws Exception {
        LocalDate date = LocalDate.of(2026, 6, 15);
        // pre-insert 2 skips for the same month
        UserJpaEntity user = userEntity;
        AreaJpaEntity area = areaJpaRepository.findById(areaId).get();
        skipDayJpaRepository.saveAndFlush(SkipDayJpaEntity.builder().user(user).area(area).skipDate(date.minusDays(1)).reason("sick").build());
        skipDayJpaRepository.saveAndFlush(SkipDayJpaEntity.builder().user(user).area(area).skipDate(date.minusDays(2)).reason("sick").build());

        SkipDayRequest request = new SkipDayRequest(date, "trip");

        mockMvc.perform(post("/api/areas/{areaId}/skip-days", areaId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void removeSkipDay_success() throws Exception {
        LocalDate date = LocalDate.now();
        UserJpaEntity user = userEntity;
        AreaJpaEntity area = areaJpaRepository.findById(areaId).get();
        skipDayJpaRepository.save(SkipDayJpaEntity.builder().user(user).area(area).skipDate(date).reason("sick").build());

        mockMvc.perform(delete("/api/areas/{areaId}/skip-days/{date}", areaId, date.toString())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        List<SkipDayJpaEntity> skipDays = skipDayJpaRepository.findAll();
        assertEquals(0, skipDays.size());
    }
}

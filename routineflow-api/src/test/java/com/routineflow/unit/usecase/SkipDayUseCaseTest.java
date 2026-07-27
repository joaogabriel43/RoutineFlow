package com.routineflow.unit.usecase;

import com.routineflow.application.dto.SkipDayResponse;
import com.routineflow.application.usecase.SkipDayUseCase;
import com.routineflow.application.usecase.SkipDayUseCase;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.SkipDayJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SkipDayJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SkipDayUseCaseTest {

    @Mock
    private SkipDayJpaRepository skipDayJpaRepository;
    @Mock
    private AreaJpaRepository areaJpaRepository;
    @Mock
    private UserJpaRepository userJpaRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private SkipDayUseCase skipDayUseCase;
    private UserJpaEntity user;
    private AreaJpaEntity area;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache(any())).thenReturn(cache);
        skipDayUseCase = new SkipDayUseCase(skipDayJpaRepository, areaJpaRepository, userJpaRepository, cacheManager);
        user = UserJpaEntity.builder().id(1L).build();
        area = AreaJpaEntity.builder().id(10L).user(user).build();
    }

    @Test
    void skipDay_success() {
        LocalDate date = LocalDate.now();
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(areaJpaRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(area));
        when(skipDayJpaRepository.findByUserIdAndAreaIdAndSkipDate(1L, 10L, date)).thenReturn(Optional.empty());
        when(skipDayJpaRepository.countByUserIdAndAreaIdAndSkipDateBetween(1L, 10L, startOfMonth, endOfMonth)).thenReturn(1L);

        SkipDayJpaEntity saved = SkipDayJpaEntity.builder().id(100L).user(user).area(area).skipDate(date).reason("vacation").build();
        when(skipDayJpaRepository.save(any())).thenReturn(saved);

        SkipDayResponse response = skipDayUseCase.skipDay(1L, 10L, date, "vacation");

        assertEquals(100L, response.id());
        assertEquals("vacation", response.reason());
        
        ArgumentCaptor<SkipDayJpaEntity> captor = ArgumentCaptor.forClass(SkipDayJpaEntity.class);
        verify(skipDayJpaRepository).save(captor.capture());
        assertEquals(date, captor.getValue().getSkipDate());
    }

    @Test
    void skipDay_throwsIfAlreadySkipped() {
        LocalDate date = LocalDate.now();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(areaJpaRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(area));
        when(skipDayJpaRepository.findByUserIdAndAreaIdAndSkipDate(1L, 10L, date)).thenReturn(Optional.of(new SkipDayJpaEntity()));

        assertThrows(IllegalStateException.class, () -> skipDayUseCase.skipDay(1L, 10L, date, "sick"));
    }

    @Test
    void skipDay_throwsIfMoreThan2PerMonth() {
        LocalDate date = LocalDate.now();
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(areaJpaRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(area));
        when(skipDayJpaRepository.findByUserIdAndAreaIdAndSkipDate(1L, 10L, date)).thenReturn(Optional.empty());
        when(skipDayJpaRepository.countByUserIdAndAreaIdAndSkipDateBetween(1L, 10L, startOfMonth, endOfMonth)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> skipDayUseCase.skipDay(1L, 10L, date, "sick"));
    }

    @Test
    void skipDay_throwsIfFutureBeyondTomorrow() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(areaJpaRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(area));

        assertThrows(IllegalArgumentException.class, () -> skipDayUseCase.skipDay(1L, 10L, date, "vacation"));
    }
}

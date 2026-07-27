package com.routineflow.application.usecase;

import com.routineflow.application.dto.SkipDayResponse;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.SkipDayJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SkipDayJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.routineflow.infrastructure.config.CacheConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkipDayUseCase {

    private final SkipDayJpaRepository skipDayJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CacheManager cacheManager;

    public SkipDayUseCase(SkipDayJpaRepository skipDayJpaRepository,
                          AreaJpaRepository areaJpaRepository,
                          UserJpaRepository userJpaRepository,
                          CacheManager cacheManager) {
        this.skipDayJpaRepository = skipDayJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public SkipDayResponse skipDay(Long userId, Long areaId, LocalDate date, String reason) {
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AreaJpaEntity area = areaJpaRepository.findByIdAndUserId(areaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found for this user"));

        LocalDate today = LocalDate.now();
        if (date.isAfter(today.plusDays(1))) {
            throw new IllegalArgumentException("Cannot skip a day further in the future than tomorrow.");
        }

        if (date.isBefore(today.minusDays(7))) {
            throw new IllegalArgumentException("Cannot skip a day more than 7 days in the past.");
        }

        if (isSkipDay(userId, areaId, date)) {
            throw new IllegalStateException("Area is already skipped for this date.");
        }

        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());

        long countThisMonth = skipDayJpaRepository.countByUserIdAndAreaIdAndSkipDateBetween(
                userId, areaId, startOfMonth, endOfMonth);

        if (countThisMonth >= 2) {
            throw new IllegalStateException("Limite de 2 skip days por mês atingido");
        }

        SkipDayJpaEntity skipDay = SkipDayJpaEntity.builder()
                .user(user)
                .area(area)
                .skipDate(date)
                .reason(reason)
                .build();

        skipDay = skipDayJpaRepository.save(skipDay);

        evictUserCaches(userId, areaId);

        return new SkipDayResponse(
                skipDay.getId(),
                skipDay.getArea().getId(),
                skipDay.getSkipDate(),
                skipDay.getReason(),
                skipDay.getCreatedAt()
        );
    }

    @Transactional
    public void removeSkipDay(Long userId, Long areaId, LocalDate date) {
        skipDayJpaRepository.findByUserIdAndAreaIdAndSkipDate(userId, areaId, date)
                .ifPresent(skipDayJpaRepository::delete);
        evictUserCaches(userId, areaId);
    }

    @Transactional(readOnly = true)
    public boolean isSkipDay(Long userId, Long areaId, LocalDate date) {
        return skipDayJpaRepository.findByUserIdAndAreaIdAndSkipDate(userId, areaId, date).isPresent();
    }

    @Transactional(readOnly = true)
    public List<SkipDayResponse> listSkipDays(Long userId, Long areaId) {
        return skipDayJpaRepository.findAll().stream() // In a real app we would have findAllByUserIdAndAreaId
                .filter(s -> s.getUser().getId().equals(userId) && s.getArea().getId().equals(areaId))
                .map(s -> new SkipDayResponse(
                        s.getId(),
                        s.getArea().getId(),
                        s.getSkipDate(),
                        s.getReason(),
                        s.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private void evictUserCaches(Long userId, Long areaId) {
        if (cacheManager.getCache(CacheConfig.CACHE_HEATMAP) != null) {
            cacheManager.getCache(CacheConfig.CACHE_HEATMAP).evict(userId);
        }
        if (cacheManager.getCache(CacheConfig.CACHE_STREAK) != null) {
            cacheManager.getCache(CacheConfig.CACHE_STREAK).evict(userId);
        }
        if (areaId != null && cacheManager.getCache(CacheConfig.CACHE_ANALYTICS) != null) {
            cacheManager.getCache(CacheConfig.CACHE_ANALYTICS).evict(userId + "-" + areaId);
        }
    }
}

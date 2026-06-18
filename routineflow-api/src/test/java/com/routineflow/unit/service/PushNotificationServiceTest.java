package com.routineflow.unit.service;

import com.routineflow.application.usecase.PushNotificationService;
import com.routineflow.infrastructure.config.VapidProperties;
import com.routineflow.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.PushSubscriptionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock private PushSubscriptionJpaRepository subscriptionRepository;
    @Mock private VapidProperties vapidProperties;

    private PushNotificationService service;

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(subscriptionRepository, vapidProperties);
        // Don't call init() — pushService will be null, which means sendNotification is a no-op.
        // This tests the guard-clause behavior.
    }

    @Test
    @DisplayName("sendNotification_userWithNoSubscriptions_doesNotThrow")
    void sendNotification_userWithNoSubscriptions_doesNotThrow() {
        // pushService is null (not configured) — should be a safe no-op
        assertThatNoException().isThrownBy(() ->
                service.sendNotification(1L, "Title", "Body"));
    }

    @Test
    @DisplayName("sendNotification_pushNotConfigured_doesNotThrow")
    void sendNotification_pushNotConfigured_doesNotThrow() {
        // Even without calling init(), sendNotification should be a no-op
        assertThatNoException().isThrownBy(() ->
                service.sendNotification(999L, "Test", "Test body"));

        // subscriptionRepository should never be called when pushService is null
        verifyNoInteractions(subscriptionRepository);
    }
}

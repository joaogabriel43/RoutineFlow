package com.routineflow.presentation.controller;

import com.routineflow.application.dto.SubscribeRequest;
import com.routineflow.application.dto.UnsubscribeRequest;
import com.routineflow.infrastructure.config.VapidProperties;
import com.routineflow.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.routineflow.infrastructure.persistence.repository.PushSubscriptionJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Push Notifications", description = "Web Push subscription management")
@RestController
@RequestMapping("/push")
public class PushNotificationController {

    private final VapidProperties vapidProperties;
    private final PushSubscriptionJpaRepository subscriptionRepository;
    private final UserJpaRepository userJpaRepository;
    private final AuthenticatedUserResolver userResolver;

    public PushNotificationController(
            VapidProperties vapidProperties,
            PushSubscriptionJpaRepository subscriptionRepository,
            UserJpaRepository userJpaRepository,
            AuthenticatedUserResolver userResolver
    ) {
        this.vapidProperties = vapidProperties;
        this.subscriptionRepository = subscriptionRepository;
        this.userJpaRepository = userJpaRepository;
        this.userResolver = userResolver;
    }

    @Operation(summary = "Get the VAPID public key for push subscription")
    @GetMapping("/vapid-public-key")
    public ResponseEntity<String> getVapidPublicKey() {
        String key = vapidProperties.getPublicKey();
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Push not configured");
        }
        return ResponseEntity.ok(key);
    }

    @Operation(summary = "Subscribe a device for push notifications")
    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Long userId = userResolver.currentUserId();
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        // Upsert: if endpoint already exists, update keys (device may have rotated)
        var existing = subscriptionRepository.findByEndpoint(request.endpoint());
        if (existing.isPresent()) {
            var sub = existing.get();
            sub.setP256dh(request.p256dh());
            sub.setAuth(request.auth());
            sub.setUser(user);
            subscriptionRepository.save(sub);
        } else {
            var sub = PushSubscriptionJpaEntity.builder()
                    .user(user)
                    .endpoint(request.endpoint())
                    .p256dh(request.p256dh())
                    .auth(request.auth())
                    .build();
            subscriptionRepository.save(sub);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Unsubscribe a device from push notifications")
    @DeleteMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
        subscriptionRepository.deleteByEndpoint(request.endpoint());
        return ResponseEntity.noContent().build();
    }
}

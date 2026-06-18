package com.routineflow.application.usecase;

import com.routineflow.infrastructure.config.VapidProperties;
import com.routineflow.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.routineflow.infrastructure.persistence.repository.PushSubscriptionJpaRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionJpaRepository subscriptionRepository;
    private final VapidProperties vapidProperties;
    private PushService pushService;

    public PushNotificationService(
            PushSubscriptionJpaRepository subscriptionRepository,
            VapidProperties vapidProperties
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.vapidProperties = vapidProperties;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (vapidProperties.isConfigured()) {
            try {
                pushService = new PushService()
                        .setPublicKey(vapidProperties.getPublicKey())
                        .setPrivateKey(vapidProperties.getPrivateKey())
                        .setSubject(vapidProperties.getSubject());
                log.info("Web Push service initialized with VAPID keys");
            } catch (GeneralSecurityException e) {
                log.error("Failed to initialize Web Push service: {}", e.getMessage());
                throw e;
            }
        } else {
            log.warn("VAPID keys not configured — push notifications disabled");
        }
    }

    /**
     * Sends a push notification to all subscriptions of the given user.
     * If an endpoint returns 410 Gone (subscription expired), it is automatically deleted.
     * If the user has no subscriptions, this is a no-op.
     */
    public void sendNotification(Long userId, String title, String body) {
        if (pushService == null) {
            log.debug("Push service not configured — skipping notification for userId={}", userId);
            return;
        }

        List<PushSubscriptionJpaEntity> subscriptions = subscriptionRepository.findAllByUserId(userId);
        if (subscriptions.isEmpty()) {
            log.debug("No push subscriptions found for userId={}", userId);
            return;
        }

        String payload = String.format(
                "{\"title\":\"%s\",\"body\":\"%s\",\"icon\":\"/icons/icon-192.png\"}",
                escapeJson(title), escapeJson(body));

        for (PushSubscriptionJpaEntity sub : subscriptions) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes()
                );
                var response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 410 || statusCode == 404) {
                    // Subscription expired or invalid — clean up
                    log.info("Push subscription expired ({}), removing endpoint for userId={}",
                            statusCode, userId);
                    deleteSubscription(sub.getEndpoint());
                } else if (statusCode >= 400) {
                    log.warn("Push notification failed with status {} for userId={}", statusCode, userId);
                }
            } catch (Exception e) {
                log.error("Error sending push notification to userId={}: {}", userId, e.getMessage());
            }
        }
    }

    @Transactional
    public void deleteSubscription(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r");
    }
}

package com.routineflow.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VAPID keys configuration for Web Push notifications.
 *
 * <p>Keys are generated via {@code npx web-push generate-vapid-keys}
 * and injected via environment variables VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.vapid")
public class VapidProperties {

    private String publicKey;
    private String privateKey;
    private String subject;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isConfigured() {
        return publicKey != null && !publicKey.isBlank()
                && privateKey != null && !privateKey.isBlank();
    }
}

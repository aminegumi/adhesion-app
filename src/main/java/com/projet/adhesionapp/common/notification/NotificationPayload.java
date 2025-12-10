package com.projet.adhesionapp.common.notification;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

/**
 * Payload for sending notifications across different channels
 */
@Data
@Builder
public class NotificationPayload {
    private Long userId;
    private String title;
    private String body;
    private NotificationType type;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    private String actionUrl;
    private String imageUrl;

    @Singular
    private Map<String, String> data;
}

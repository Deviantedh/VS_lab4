package portal.notification.application.port.in;

import portal.notification.domain.model.Notification;

public interface MarkNotificationReadUseCase {
    Notification markAsRead(Long id, Long userId, String userRole);
    void markAllAsRead(Long userId, String userRole);
}

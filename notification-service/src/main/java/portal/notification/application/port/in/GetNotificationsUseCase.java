package portal.notification.application.port.in;

import portal.notification.domain.model.Notification;

import java.util.List;

public interface GetNotificationsUseCase {
    List<Notification> getNotificationsForUser(Long userId, String role, Boolean isRead, int page, int size);
    long countUnread(Long userId, String role);
}

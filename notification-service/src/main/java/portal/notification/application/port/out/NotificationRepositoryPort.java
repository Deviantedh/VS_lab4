package portal.notification.application.port.out;

import portal.notification.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findAllByRecipient(Long userId, String role, Boolean isRead, int page, int size);
    long countUnreadByRecipient(Long userId, String role);
    void markAllReadForUser(Long userId, String role, LocalDateTime readAt);
}

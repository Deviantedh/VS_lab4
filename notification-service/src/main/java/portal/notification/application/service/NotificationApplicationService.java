package portal.notification.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.notification.application.port.in.CreateNotificationUseCase;
import portal.notification.application.port.in.GetNotificationsUseCase;
import portal.notification.application.port.in.MarkNotificationReadUseCase;
import portal.notification.application.port.out.NotificationRepositoryPort;
import portal.notification.domain.exception.NotificationNotFoundException;
import portal.notification.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationApplicationService implements GetNotificationsUseCase, MarkNotificationReadUseCase, CreateNotificationUseCase {

    private final NotificationRepositoryPort repositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId, String role, Boolean isRead, int page, int size) {
        if (size <= 0 || size > 50) {
            size = 20;
        }
        if (page < 0) {
            page = 0;
        }
        return repositoryPort.findAllByRecipient(userId, role, isRead, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId, String role) {
        return repositoryPort.countUnreadByRecipient(userId, role);
    }

    @Override
    public Notification markAsRead(Long id, Long userId, String userRole) {
        Notification notification = repositoryPort.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        boolean canAccess = "ADMIN".equalsIgnoreCase(userRole)
                || (notification.getRecipientUserId() != null && notification.getRecipientUserId().equals(userId))
                || (notification.getTargetRole() != null && notification.getTargetRole().equalsIgnoreCase(userRole));

        if (!canAccess) {
            throw new AccessDeniedException("Нет доступа к чужому уведомлению");
        }

        notification.markAsRead();
        return repositoryPort.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId, String userRole) {
        repositoryPort.markAllReadForUser(userId, userRole, LocalDateTime.now());
    }

    @Override
    public Notification create(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification = notification.toBuilder().createdAt(LocalDateTime.now()).build();
        }
        return repositoryPort.save(notification);
    }
}

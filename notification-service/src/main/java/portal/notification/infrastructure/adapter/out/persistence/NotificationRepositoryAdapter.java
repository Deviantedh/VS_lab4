package portal.notification.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import portal.notification.application.port.out.NotificationRepositoryPort;
import portal.notification.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final SpringDataNotificationRepository repository;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = toEntity(notification);
        NotificationJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findAllByRecipient(Long userId, String role, Boolean isRead, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return repository.findByRecipient(userId, role, isRead, pageRequest).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByRecipient(Long userId, String role) {
        return repository.countUnread(userId, role);
    }

    @Override
    public void markAllReadForUser(Long userId, String role, LocalDateTime readAt) {
        repository.markAllRead(userId, role, readAt);
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        if (entity == null) return null;
        return Notification.builder()
                .id(entity.getId())
                .recipientUserId(entity.getRecipientUserId())
                .recipientEmployeeId(entity.getRecipientEmployeeId())
                .targetRole(entity.getTargetRole())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .link(entity.getLink())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }

    private NotificationJpaEntity toEntity(Notification domain) {
        if (domain == null) return null;
        return NotificationJpaEntity.builder()
                .id(domain.getId())
                .recipientUserId(domain.getRecipientUserId())
                .recipientEmployeeId(domain.getRecipientEmployeeId())
                .targetRole(domain.getTargetRole())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .type(domain.getType())
                .link(domain.getLink())
                .isRead(domain.isRead())
                .createdAt(domain.getCreatedAt())
                .readAt(domain.getReadAt())
                .build();
    }
}

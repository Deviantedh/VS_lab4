package portal.notification.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {

    @Mock
    private SpringDataNotificationRepository repository;

    private NotificationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationRepositoryAdapter(repository);
    }

    @Test
    @DisplayName("NotificationRepositoryAdapter maps and saves domain notification")
    void testSave() {
        Notification domain = Notification.builder()
                .recipientUserId(1L)
                .title("Заголовок")
                .message("Сообщение")
                .type(NotificationType.INFO)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(10L)
                .recipientUserId(1L)
                .title("Заголовок")
                .message("Сообщение")
                .type(NotificationType.INFO)
                .isRead(false)
                .createdAt(domain.getCreatedAt())
                .build();

        when(repository.save(any(NotificationJpaEntity.class))).thenReturn(entity);

        Notification saved = adapter.save(domain);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getTitle()).isEqualTo("Заголовок");
    }

    @Test
    @DisplayName("NotificationRepositoryAdapter finds by id")
    void testFindById() {
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(5L)
                .title("Уведомление")
                .type(NotificationType.WARNING)
                .build();

        when(repository.findById(5L)).thenReturn(Optional.of(entity));

        Optional<Notification> result = adapter.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(5L);
        assertThat(result.get().getType()).isEqualTo(NotificationType.WARNING);
    }

    @Test
    @DisplayName("NotificationRepositoryAdapter finds all by recipient")
    void testFindAllByRecipient() {
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(1L)
                .recipientUserId(2L)
                .title("Заголовок")
                .type(NotificationType.INFO)
                .build();

        when(repository.findByRecipient(eq(2L), eq("EMPLOYEE"), eq(false), any(Pageable.class)))
                .thenReturn(List.of(entity));

        List<Notification> list = adapter.findAllByRecipient(2L, "EMPLOYEE", false, 0, 10);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("NotificationRepositoryAdapter counts unread")
    void testCountUnread() {
        when(repository.countUnread(2L, "EMPLOYEE")).thenReturn(7L);

        long count = adapter.countUnreadByRecipient(2L, "EMPLOYEE");

        assertThat(count).isEqualTo(7L);
    }

    @Test
    @DisplayName("NotificationRepositoryAdapter marks all read")
    void testMarkAllRead() {
        LocalDateTime now = LocalDateTime.now();
        adapter.markAllReadForUser(2L, "EMPLOYEE", now);

        verify(repository).markAllRead(2L, "EMPLOYEE", now);
    }
}

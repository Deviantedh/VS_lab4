package portal.notification.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import portal.notification.application.port.out.NotificationRepositoryPort;
import portal.notification.application.service.NotificationApplicationService;
import portal.notification.domain.exception.NotificationNotFoundException;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private NotificationRepositoryPort repositoryPort;

    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(repositoryPort);
    }

    @Test
    @DisplayName("getNotificationsForUser normalizes page and size limits")
    void testGetNotificationsForUser() {
        Notification notification = Notification.builder().id(1L).title("Тест").build();
        when(repositoryPort.findAllByRecipient(eq(1L), eq("HR"), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(notification));

        List<Notification> result = service.getNotificationsForUser(1L, "HR", null, -5, 100);

        assertThat(result).hasSize(1);
        verify(repositoryPort).findAllByRecipient(1L, "HR", null, 0, 20);
    }

    @Test
    @DisplayName("countUnread delegates to repository port")
    void testCountUnread() {
        when(repositoryPort.countUnreadByRecipient(1L, "ADMIN")).thenReturn(5L);

        long count = service.countUnread(1L, "ADMIN");

        assertThat(count).isEqualTo(5L);
        verify(repositoryPort).countUnreadByRecipient(1L, "ADMIN");
    }

    @Test
    @DisplayName("markAsRead succeeds for owner user")
    void testMarkAsReadSuccess() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientUserId(2L)
                .title("Тест")
                .isRead(false)
                .build();

        when(repositoryPort.findById(1L)).thenReturn(Optional.of(notification));
        when(repositoryPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification updated = service.markAsRead(1L, 2L, "EMPLOYEE");

        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("markAsRead throws AccessDeniedException when another user attempts to read")
    void testMarkAsReadAccessDenied() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientUserId(2L)
                .title("Тест")
                .isRead(false)
                .build();

        when(repositoryPort.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(1L, 99L, "EMPLOYEE"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Нет доступа");
    }

    @Test
    @DisplayName("markAsRead throws NotificationNotFoundException when ID does not exist")
    void testMarkAsReadNotFound() {
        when(repositoryPort.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(404L, 1L, "ADMIN"))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("markAllAsRead delegates to repository port")
    void testMarkAllAsRead() {
        service.markAllAsRead(2L, "EMPLOYEE");
        verify(repositoryPort).markAllReadForUser(eq(2L), eq("EMPLOYEE"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("create sets createdAt if null and saves")
    void testCreate() {
        Notification notification = Notification.builder()
                .title("Новое")
                .type(NotificationType.INFO)
                .build();

        when(repositoryPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification created = service.create(notification);

        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Новое");
    }
}

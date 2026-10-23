package portal.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDomainTest {

    @Test
    @DisplayName("Notification domain entity marks as read and updates readAt timestamp")
    void testMarkAsRead() {
        Notification notification = Notification.builder()
                .id(1L)
                .recipientUserId(5L)
                .title("Тест")
                .message("Сообщение")
                .type(NotificationType.INFO)
                .isRead(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Notification belongsToUser checks recipient user ID correctly")
    void testBelongsToUser() {
        Notification notification = Notification.builder()
                .id(2L)
                .recipientUserId(10L)
                .build();

        assertThat(notification.belongsToUser(10L)).isTrue();
        assertThat(notification.belongsToUser(99L)).isFalse();

        Notification roleNotification = Notification.builder()
                .id(3L)
                .targetRole("MANAGER")
                .build();

        assertThat(roleNotification.belongsToUser(10L)).isFalse();
    }
}

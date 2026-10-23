package portal.notification.infrastructure.adapter.in.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.NotificationDto;
import portal.dto.RoleCode;
import portal.notification.application.port.in.CreateNotificationUseCase;
import portal.notification.application.port.in.GetNotificationsUseCase;
import portal.notification.application.port.in.MarkNotificationReadUseCase;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;
import portal.notification.infrastructure.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private GetNotificationsUseCase getNotificationsUseCase;

    @Mock
    private MarkNotificationReadUseCase markNotificationReadUseCase;

    @Mock
    private CreateNotificationUseCase createNotificationUseCase;

    @InjectMocks
    private NotificationController controller;

    @Test
    @DisplayName("getNotifications returns list of NotificationDto for authenticated user")
    void testGetNotifications() {
        UserPrincipal principal = UserPrincipal.fromClaims(1L, "admin", RoleCode.ADMIN, null);
        Notification notification = Notification.builder()
                .id(1L)
                .recipientUserId(1L)
                .title("Тестовое уведомление")
                .message("Тестовый текст")
                .type(NotificationType.INFO)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(getNotificationsUseCase.getNotificationsForUser(eq(1L), eq("ADMIN"), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(notification));

        ResponseEntity<List<NotificationDto>> response = controller.getNotifications(principal, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Тестовое уведомление");
    }

    @Test
    @DisplayName("getUnreadCount returns map with count")
    void testGetUnreadCount() {
        UserPrincipal principal = UserPrincipal.fromClaims(2L, "employee", RoleCode.EMPLOYEE, 5L);
        when(getNotificationsUseCase.countUnread(2L, "EMPLOYEE")).thenReturn(3L);

        ResponseEntity<Map<String, Long>> response = controller.getUnreadCount(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("unreadCount")).isEqualTo(3L);
    }

    @Test
    @DisplayName("markAsRead marks notification as read and returns DTO")
    void testMarkAsRead() {
        UserPrincipal principal = UserPrincipal.fromClaims(2L, "employee", RoleCode.EMPLOYEE, 5L);
        Notification updated = Notification.builder()
                .id(10L)
                .recipientUserId(2L)
                .title("Уведомление")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(markNotificationReadUseCase.markAsRead(10L, 2L, "EMPLOYEE")).thenReturn(updated);

        ResponseEntity<NotificationDto> response = controller.markAsRead(10L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isRead()).isTrue();
    }

    @Test
    @DisplayName("markAllRead invokes markAllAsRead on use case")
    void testMarkAllRead() {
        UserPrincipal principal = UserPrincipal.fromClaims(2L, "employee", RoleCode.EMPLOYEE, 5L);

        ResponseEntity<Map<String, String>> response = controller.markAllRead(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("SUCCESS");
        verify(markNotificationReadUseCase).markAllAsRead(2L, "EMPLOYEE");
    }

    @Test
    @DisplayName("createNotification creates notification and returns DTO")
    void testCreateNotification() {
        UserPrincipal principal = UserPrincipal.fromClaims(1L, "admin", RoleCode.ADMIN, null);
        NotificationDto requestDto = NotificationDto.builder()
                .recipientUserId(2L)
                .title("Новое уведомление")
                .message("Текст")
                .type("SUCCESS")
                .link("/test")
                .build();

        Notification created = Notification.builder()
                .id(100L)
                .recipientUserId(2L)
                .title("Новое уведомление")
                .message("Текст")
                .type(NotificationType.SUCCESS)
                .link("/test")
                .createdAt(LocalDateTime.now())
                .build();

        when(createNotificationUseCase.create(any(Notification.class))).thenReturn(created);

        ResponseEntity<NotificationDto> response = controller.createNotification(requestDto, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(100L);
        assertThat(response.getBody().getType()).isEqualTo("SUCCESS");
    }
}

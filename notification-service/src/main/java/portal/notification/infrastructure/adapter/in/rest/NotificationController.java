package portal.notification.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portal.dto.NotificationDto;
import portal.notification.application.port.in.CreateNotificationUseCase;
import portal.notification.application.port.in.GetNotificationsUseCase;
import portal.notification.application.port.in.MarkNotificationReadUseCase;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;
import portal.notification.infrastructure.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления", description = "Управление персональными и системными уведомлениями (Clean Architecture)")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final CreateNotificationUseCase createNotificationUseCase;

    @GetMapping
    @Operation(summary = "Получить список уведомлений текущего пользователя с пагинацией")
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null && principal.getRole() != null ? principal.getRole().name() : "EMPLOYEE";

        List<Notification> notifications = getNotificationsUseCase.getNotificationsForUser(userId, role, isRead, page, size);
        List<NotificationDto> dtos = notifications.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Получить количество непрочитанных уведомлений")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null && principal.getRole() != null ? principal.getRole().name() : "EMPLOYEE";

        long count = getNotificationsUseCase.countUnread(userId, role);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Пометить уведомление как прочитанное")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null && principal.getRole() != null ? principal.getRole().name() : "EMPLOYEE";

        Notification updated = markNotificationReadUseCase.markAsRead(id, userId, role);
        return ResponseEntity.ok(toDto(updated));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Пометить все уведомления как прочитанные")
    public ResponseEntity<Map<String, String>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null && principal.getRole() != null ? principal.getRole().name() : "EMPLOYEE";

        markNotificationReadUseCase.markAllAsRead(userId, role);
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @PostMapping
    @Operation(summary = "Создать новое уведомление вручную (для админов/сервисов)")
    public ResponseEntity<NotificationDto> createNotification(
            @RequestBody NotificationDto requestDto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        NotificationType type = NotificationType.INFO;
        if (requestDto.getType() != null) {
            try {
                type = NotificationType.valueOf(requestDto.getType().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        Notification notification = Notification.builder()
                .recipientUserId(requestDto.getRecipientUserId())
                .title(requestDto.getTitle())
                .message(requestDto.getMessage())
                .type(type)
                .link(requestDto.getLink())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification created = createNotificationUseCase.create(notification);
        return ResponseEntity.ok(toDto(created));
    }

    private NotificationDto toDto(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : "INFO")
                .link(n.getLink())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

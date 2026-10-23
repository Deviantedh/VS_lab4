package portal.notification.infrastructure.adapter.in.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import portal.dto.event.*;
import portal.notification.application.port.in.CreateNotificationUseCase;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final CreateNotificationUseCase createNotificationUseCase;

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void handleNotificationEvent(NotificationEventDto event) {
        log.info("Received notification event: {}", event);
        NotificationType type = NotificationType.INFO;
        if (event.getType() != null) {
            try {
                type = NotificationType.valueOf(event.getType().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        Notification notification = Notification.builder()
                .recipientUserId(event.getRecipientUserId())
                .recipientEmployeeId(event.getRecipientEmployeeId())
                .targetRole(event.getTargetRole() != null ? event.getTargetRole().name() : null)
                .title(event.getTitle() != null ? event.getTitle() : "Уведомление")
                .message(event.getMessage())
                .type(type)
                .link(event.getLink())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        createNotificationUseCase.create(notification);
    }

    @KafkaListener(topics = "attendance-events", groupId = "notification-service-group")
    public void handleAttendanceEvent(AttendanceEventDto event) {
        log.info("Received attendance event: {}", event);
        if (event.getEventType() == AttendanceEventDto.EventType.LATE_ARRIVAL || (event.getLateMinutes() != null && event.getLateMinutes() > 0)) {
            Notification notification = Notification.builder()
                    .recipientEmployeeId(event.getEmployeeId())
                    .targetRole("MANAGER")
                    .title("Опоздание на смену")
                    .message(String.format("Сотрудник #%d опоздал на смену #%d на %d мин.",
                            event.getEmployeeId(), event.getShiftId(), event.getLateMinutes()))
                    .type(NotificationType.WARNING)
                    .link("/attendance")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        } else if (event.getEventType() == AttendanceEventDto.EventType.OVERTIME || (event.getOvertimeMinutes() != null && event.getOvertimeMinutes() > 0)) {
            Notification notification = Notification.builder()
                    .recipientEmployeeId(event.getEmployeeId())
                    .targetRole("MANAGER")
                    .title("Переработка на смене")
                    .message(String.format("Сотрудник #%d переработал на смене #%d на %d мин.",
                            event.getEmployeeId(), event.getShiftId(), event.getOvertimeMinutes()))
                    .type(NotificationType.INFO)
                    .link("/attendance")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        }
    }

    @KafkaListener(topics = "schedule-events", groupId = "notification-service-group")
    public void handleScheduleEvent(ScheduleEventDto event) {
        log.info("Received schedule event: {}", event);
        if (event.getEventType() == ScheduleEventDto.EventType.SHIFT_ASSIGNED) {
            Notification notification = Notification.builder()
                    .recipientEmployeeId(event.getEmployeeId())
                    .title("Назначение на смену")
                    .message(String.format("Вам назначена плановая смена на дату %s (Смена #%d)",
                            event.getShiftDate(), event.getShiftId()))
                    .type(NotificationType.SUCCESS)
                    .link("/shifts")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        } else if (event.getEventType() == ScheduleEventDto.EventType.REQUEST_APPROVED) {
            Notification notification = Notification.builder()
                    .recipientEmployeeId(event.getEmployeeId())
                    .title("Заявка одобрена")
                    .message(String.format("Ваша заявка #%d (%s) была успешно одобрена руководителем.",
                            event.getRequestId(), event.getRequestType()))
                    .type(NotificationType.SUCCESS)
                    .link("/requests")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        } else if (event.getEventType() == ScheduleEventDto.EventType.REQUEST_REJECTED) {
            Notification notification = Notification.builder()
                    .recipientEmployeeId(event.getEmployeeId())
                    .title("Заявка отклонена")
                    .message(String.format("Ваша заявка #%d (%s) была отклонена.",
                            event.getRequestId(), event.getRequestType()))
                    .type(NotificationType.ALERT)
                    .link("/requests")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        }
    }

    @KafkaListener(topics = "employee-events", groupId = "notification-service-group")
    public void handleEmployeeEvent(EmployeeEventDto event) {
        log.info("Received employee event: {}", event);
        if (event.getEventType() == EmployeeEventDto.EventType.EMPLOYEE_CREATED) {
            Notification notification = Notification.builder()
                    .targetRole("HR")
                    .title("Новый сотрудник в штате")
                    .message(String.format("В штат добавлен новый сотрудник: %s (%s)",
                            event.getFullName(), event.getPhone()))
                    .type(NotificationType.INFO)
                    .link("/employees")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        } else if (event.getEventType() == EmployeeEventDto.EventType.EMPLOYEE_DISMISSED) {
            Notification notification = Notification.builder()
                    .targetRole("HR")
                    .title("Увольнение сотрудника")
                    .message(String.format("Сотрудник %s (ID #%d) уволен.",
                            event.getFullName(), event.getEmployeeId()))
                    .type(NotificationType.WARNING)
                    .link("/employees")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        }
    }

    @KafkaListener(topics = "file-events", groupId = "notification-service-group")
    public void handleFileEvent(FileEventDto event) {
        log.info("Received file event: {}", event);
        if (event.getEventType() == FileEventDto.EventType.FILE_UPLOADED) {
            Notification notification = Notification.builder()
                    .targetRole("ADMIN")
                    .title("Загружен новый файл")
                    .message(String.format("Пользователь %s загрузил файл: %s (%d байт)",
                            event.getUploadedBy(), event.getOriginalFilename(), event.getSize()))
                    .type(NotificationType.INFO)
                    .link("/files")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            createNotificationUseCase.create(notification);
        }
    }
}

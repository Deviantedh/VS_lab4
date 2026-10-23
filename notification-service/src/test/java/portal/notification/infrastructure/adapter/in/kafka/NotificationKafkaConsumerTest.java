package portal.notification.infrastructure.adapter.in.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.RoleCode;
import portal.dto.event.*;
import portal.notification.application.port.in.CreateNotificationUseCase;
import portal.notification.domain.model.Notification;
import portal.notification.domain.model.NotificationType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @Mock
    private CreateNotificationUseCase createNotificationUseCase;

    private NotificationKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationKafkaConsumer(createNotificationUseCase);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles NotificationEventDto correctly")
    void testHandleNotificationEvent() {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientUserId(2L)
                .title("Заявка одобрена")
                .message("Отпуск с 10 по 20")
                .type("SUCCESS")
                .targetRole(RoleCode.EMPLOYEE)
                .timestamp(Instant.now())
                .build();

        consumer.handleNotificationEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getRecipientUserId()).isEqualTo(2L);
        assertThat(created.getTitle()).isEqualTo("Заявка одобрена");
        assertThat(created.getType()).isEqualTo(NotificationType.SUCCESS);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles AttendanceEventDto with late arrival")
    void testHandleAttendanceEventLate() {
        AttendanceEventDto event = AttendanceEventDto.builder()
                .eventType(AttendanceEventDto.EventType.LATE_ARRIVAL)
                .employeeId(10L)
                .shiftId(5L)
                .lateMinutes(25)
                .actualStart(LocalDateTime.now())
                .build();

        consumer.handleAttendanceEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getTargetRole()).isEqualTo("MANAGER");
        assertThat(created.getTitle()).isEqualTo("Опоздание на смену");
        assertThat(created.getMessage()).contains("опоздал на смену #5 на 25 мин.");
        assertThat(created.getType()).isEqualTo(NotificationType.WARNING);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles ScheduleEventDto when shift assigned")
    void testHandleScheduleEventShiftAssigned() {
        ScheduleEventDto event = ScheduleEventDto.builder()
                .eventType(ScheduleEventDto.EventType.SHIFT_ASSIGNED)
                .employeeId(7L)
                .shiftId(12L)
                .shiftDate(LocalDate.of(2026, 11, 15))
                .build();

        consumer.handleScheduleEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getRecipientEmployeeId()).isEqualTo(7L);
        assertThat(created.getTitle()).isEqualTo("Назначение на смену");
        assertThat(created.getMessage()).contains("2026-11-15");
        assertThat(created.getType()).isEqualTo(NotificationType.SUCCESS);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles EmployeeEventDto when employee created")
    void testHandleEmployeeEventCreated() {
        EmployeeEventDto event = EmployeeEventDto.builder()
                .eventType(EmployeeEventDto.EventType.EMPLOYEE_CREATED)
                .employeeId(99L)
                .fullName("Петр Сидоров")
                .phone("+79991112233")
                .build();

        consumer.handleEmployeeEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getTargetRole()).isEqualTo("HR");
        assertThat(created.getMessage()).contains("Петр Сидоров");
        assertThat(created.getType()).isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles FileEventDto when file uploaded")
    void testHandleFileEventUploaded() {
        FileEventDto event = FileEventDto.builder()
                .eventType(FileEventDto.EventType.FILE_UPLOADED)
                .fileId(100L)
                .originalFilename("Больничный.pdf")
                .size(2048L)
                .uploadedBy("ivanov")
                .build();

        consumer.handleFileEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getTargetRole()).isEqualTo("ADMIN");
        assertThat(created.getMessage()).contains("Больничный.pdf");
        assertThat(created.getType()).isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles AttendanceEventDto with overtime")
    void testHandleAttendanceEventOvertime() {
        AttendanceEventDto event = AttendanceEventDto.builder()
                .eventType(AttendanceEventDto.EventType.OVERTIME)
                .employeeId(10L)
                .shiftId(5L)
                .overtimeMinutes(60)
                .build();

        consumer.handleAttendanceEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getTargetRole()).isEqualTo("MANAGER");
        assertThat(created.getTitle()).isEqualTo("Переработка на смене");
        assertThat(created.getMessage()).contains("переработал на смене #5 на 60 мин.");
        assertThat(created.getType()).isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles ScheduleEventDto when request approved and rejected")
    void testHandleScheduleEventRequests() {
        ScheduleEventDto approvedEvent = ScheduleEventDto.builder()
                .eventType(ScheduleEventDto.EventType.REQUEST_APPROVED)
                .employeeId(7L)
                .requestId(101L)
                .requestType("VACATION")
                .build();

        consumer.handleScheduleEvent(approvedEvent);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Заявка одобрена");
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.SUCCESS);

        ScheduleEventDto rejectedEvent = ScheduleEventDto.builder()
                .eventType(ScheduleEventDto.EventType.REQUEST_REJECTED)
                .employeeId(7L)
                .requestId(102L)
                .requestType("DAY_OFF")
                .build();

        consumer.handleScheduleEvent(rejectedEvent);
        verify(createNotificationUseCase, org.mockito.Mockito.times(2)).create(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Заявка отклонена");
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.ALERT);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles EmployeeEventDto when employee dismissed")
    void testHandleEmployeeEventDismissed() {
        EmployeeEventDto event = EmployeeEventDto.builder()
                .eventType(EmployeeEventDto.EventType.EMPLOYEE_DISMISSED)
                .employeeId(99L)
                .fullName("Петр Сидоров")
                .build();

        consumer.handleEmployeeEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());

        Notification created = captor.getValue();
        assertThat(created.getTargetRole()).isEqualTo("HR");
        assertThat(created.getTitle()).isEqualTo("Увольнение сотрудника");
        assertThat(created.getType()).isEqualTo(NotificationType.WARNING);
    }

    @Test
    @DisplayName("NotificationKafkaConsumer handles NotificationEventDto with unknown type")
    void testHandleNotificationEventUnknownType() {
        NotificationEventDto event = NotificationEventDto.builder()
                .recipientUserId(2L)
                .title("Тест")
                .message("Сообщение")
                .type("INVALID_TYPE_XYZ")
                .build();

        consumer.handleNotificationEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(createNotificationUseCase).create(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.INFO);
    }
}

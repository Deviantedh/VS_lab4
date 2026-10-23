package portal.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoJacksonSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("EmployeeDto Request and Response serialization & deserialization with LocalDate and EmployeeStatus")
    void testEmployeeDtoSerialization() throws JsonProcessingException {
        LocalDate birthDate = LocalDate.of(1995, 5, 20);
        LocalDate hireDate = LocalDate.of(2023, 1, 15);
        Instant now = Instant.parse("2026-10-01T12:00:00Z");

        EmployeeDto.Request request = EmployeeDto.Request.builder()
                .name("Иван Иванов")
                .phone("+79991234567")
                .birthDate(birthDate)
                .hireDate(hireDate)
                .status(EmployeeStatus.ACTIVE)
                .build();

        String reqJson = objectMapper.writeValueAsString(request);
        assertThat(reqJson).contains("\"name\":\"Иван Иванов\"");
        assertThat(reqJson).contains("\"status\":\"ACTIVE\"");
        assertThat(reqJson).contains("\"birthDate\":\"1995-05-20\"");
        assertThat(reqJson).contains("\"hireDate\":\"2023-01-15\"");

        EmployeeDto.Request deserializedReq = objectMapper.readValue(reqJson, EmployeeDto.Request.class);
        assertThat(deserializedReq.getName()).isEqualTo(request.getName());
        assertThat(deserializedReq.getPhone()).isEqualTo(request.getPhone());
        assertThat(deserializedReq.getBirthDate()).isEqualTo(birthDate);
        assertThat(deserializedReq.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

        EmployeeDto.Response response = EmployeeDto.Response.builder()
                .id(100L)
                .name("Иван Иванов")
                .phone("+79991234567")
                .birthDate(birthDate)
                .hireDate(hireDate)
                .dismissalDate(null)
                .status(EmployeeStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        String respJson = objectMapper.writeValueAsString(response);
        EmployeeDto.Response deserializedResp = objectMapper.readValue(respJson, EmployeeDto.Response.class);
        assertThat(deserializedResp.getId()).isEqualTo(100L);
        assertThat(deserializedResp.getCreatedAt()).isEqualTo(now);
        assertThat(deserializedResp.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("AttendanceRecordDto Request and Response with Instant dates")
    void testAttendanceRecordDtoSerialization() throws JsonProcessingException {
        Instant plannedStart = Instant.parse("2026-10-01T09:00:00Z");
        Instant plannedEnd = Instant.parse("2026-10-01T18:00:00Z");
        Instant actualStart = Instant.parse("2026-10-01T09:05:00Z");
        Instant actualEnd = Instant.parse("2026-10-01T18:02:00Z");

        AttendanceRecordDto.Request request = AttendanceRecordDto.Request.builder()
                .employeeId(1L)
                .shiftId(10L)
                .plannedStart(plannedStart)
                .plannedEnd(plannedEnd)
                .actualStart(actualStart)
                .actualEnd(actualEnd)
                .breakMinutes(45)
                .comment("Своевременно")
                .build();

        String json = objectMapper.writeValueAsString(request);
        AttendanceRecordDto.Request deserialized = objectMapper.readValue(json, AttendanceRecordDto.Request.class);

        assertThat(deserialized.getEmployeeId()).isEqualTo(1L);
        assertThat(deserialized.getPlannedStart()).isEqualTo(plannedStart);
        assertThat(deserialized.getActualStart()).isEqualTo(actualStart);
        assertThat(deserialized.getBreakMinutes()).isEqualTo(45);
        assertThat(deserialized.getComment()).isEqualTo("Своевременно");

        AttendanceRecordDto.CheckInRequest checkIn = AttendanceRecordDto.CheckInRequest.builder()
                .employeeId(2L)
                .shiftId(20L)
                .comment("Прибыл")
                .build();
        String checkInJson = objectMapper.writeValueAsString(checkIn);
        AttendanceRecordDto.CheckInRequest desCheckIn = objectMapper.readValue(checkInJson, AttendanceRecordDto.CheckInRequest.class);
        assertThat(desCheckIn.getEmployeeId()).isEqualTo(2L);

        AttendanceRecordDto.CheckOutRequest checkOut = AttendanceRecordDto.CheckOutRequest.builder()
                .breakMinutes(30)
                .comment("Ушел")
                .build();
        String checkOutJson = objectMapper.writeValueAsString(checkOut);
        AttendanceRecordDto.CheckOutRequest desCheckOut = objectMapper.readValue(checkOutJson, AttendanceRecordDto.CheckOutRequest.class);
        assertThat(desCheckOut.getBreakMinutes()).isEqualTo(30);

        AttendanceRecordDto.Response response = AttendanceRecordDto.Response.builder()
                .id(50L)
                .employeeId(1L)
                .employeeName("Петр Петров")
                .shiftId(10L)
                .plannedStart(plannedStart)
                .plannedEnd(plannedEnd)
                .actualStart(actualStart)
                .actualEnd(actualEnd)
                .breakMinutes(45)
                .lateMinutes(5L)
                .overtimeMinutes(2L)
                .createdAt(plannedStart)
                .updatedAt(actualEnd)
                .build();

        String respJson = objectMapper.writeValueAsString(response);
        AttendanceRecordDto.Response desResp = objectMapper.readValue(respJson, AttendanceRecordDto.Response.class);
        assertThat(desResp.getId()).isEqualTo(50L);
        assertThat(desResp.getLateMinutes()).isEqualTo(5L);
        assertThat(desResp.getOvertimeMinutes()).isEqualTo(2L);
    }

    @Test
    @DisplayName("ShiftDto and ScheduleDto with LocalDate and LocalTime")
    void testShiftDtoSerialization() throws JsonProcessingException {
        LocalDate date = LocalDate.of(2026, 10, 15);
        LocalTime timeFrom = LocalTime.of(8, 30, 0);
        LocalTime timeTo = LocalTime.of(17, 30, 0);

        ShiftDto.Request request = ShiftDto.Request.builder()
                .scheduleId(5L)
                .date(date)
                .timeFrom(timeFrom)
                .timeTo(timeTo)
                .breakMinutes(60)
                .build();

        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"date\":\"2026-10-15\"");
        assertThat(json).contains("\"timeFrom\":\"08:30:00\"");
        assertThat(json).contains("\"timeTo\":\"17:30:00\"");

        ShiftDto.Request deserialized = objectMapper.readValue(json, ShiftDto.Request.class);
        assertThat(deserialized.getScheduleId()).isEqualTo(5L);
        assertThat(deserialized.getDate()).isEqualTo(date);
        assertThat(deserialized.getTimeFrom()).isEqualTo(timeFrom);
        assertThat(deserialized.getTimeTo()).isEqualTo(timeTo);

        ShiftDto.AssignEmployeeRequest assignRequest = ShiftDto.AssignEmployeeRequest.builder()
                .employeeId(42L)
                .assignedById(1L)
                .build();
        String assignJson = objectMapper.writeValueAsString(assignRequest);
        ShiftDto.AssignEmployeeRequest desAssign = objectMapper.readValue(assignJson, ShiftDto.AssignEmployeeRequest.class);
        assertThat(desAssign.getEmployeeId()).isEqualTo(42L);

        ScheduleDto.Request scheduleReq = ScheduleDto.Request.builder()
                .branchId(1L)
                .dateFrom(LocalDate.of(2026, 1, 1))
                .dateTo(LocalDate.of(2026, 12, 31))
                .createdById(10L)
                .build();
        String schedJson = objectMapper.writeValueAsString(scheduleReq);
        ScheduleDto.Request desSched = objectMapper.readValue(schedJson, ScheduleDto.Request.class);
        assertThat(desSched.getBranchId()).isEqualTo(1L);
        assertThat(desSched.getDateFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(desSched.getDateTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Enums serialization and deserialization")
    void testEnumsSerialization() throws JsonProcessingException {
        // RoleCode
        for (RoleCode role : RoleCode.values()) {
            String json = objectMapper.writeValueAsString(role);
            assertThat(json).isEqualTo("\"" + role.name() + "\"");
            assertThat(objectMapper.readValue(json, RoleCode.class)).isEqualTo(role);
        }

        // EmployeeStatus
        for (EmployeeStatus status : EmployeeStatus.values()) {
            String json = objectMapper.writeValueAsString(status);
            assertThat(json).isEqualTo("\"" + status.name() + "\"");
            assertThat(objectMapper.readValue(json, EmployeeStatus.class)).isEqualTo(status);
        }

        // AbsenceType
        for (AbsenceType absence : AbsenceType.values()) {
            String json = objectMapper.writeValueAsString(absence);
            assertThat(json).isEqualTo("\"" + absence.name() + "\"");
            assertThat(objectMapper.readValue(json, AbsenceType.class)).isEqualTo(absence);
        }

        // RequestStatus
        for (RequestStatus status : RequestStatus.values()) {
            String json = objectMapper.writeValueAsString(status);
            assertThat(json).isEqualTo("\"" + status.name() + "\"");
            assertThat(objectMapper.readValue(json, RequestStatus.class)).isEqualTo(status);
        }

        // RequestType
        for (RequestType type : RequestType.values()) {
            String json = objectMapper.writeValueAsString(type);
            assertThat(json).isEqualTo("\"" + type.name() + "\"");
            assertThat(objectMapper.readValue(json, RequestType.class)).isEqualTo(type);
        }

        // ShiftAction
        for (ShiftAction action : ShiftAction.values()) {
            String json = objectMapper.writeValueAsString(action);
            assertThat(json).isEqualTo("\"" + action.name() + "\"");
            assertThat(objectMapper.readValue(json, ShiftAction.class)).isEqualTo(action);
        }
    }

    @Test
    @DisplayName("EmployeeAbsenceDto and RequestDto serialization with enums and dates")
    void testAbsenceAndRequestDtoSerialization() throws JsonProcessingException {
        EmployeeAbsenceDto.Request absenceReq = EmployeeAbsenceDto.Request.builder()
                .employeeId(3L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 14))
                .comment("Ежегодный отпуск")
                .build();
        String absenceJson = objectMapper.writeValueAsString(absenceReq);
        EmployeeAbsenceDto.Request desAbsence = objectMapper.readValue(absenceJson, EmployeeAbsenceDto.Request.class);
        assertThat(desAbsence.getType()).isEqualTo(AbsenceType.VACATION);
        assertThat(desAbsence.getDateFrom()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(desAbsence.getDateTo()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(desAbsence.getComment()).isEqualTo("Ежегодный отпуск");

        RequestDto.Create reqCreate = RequestDto.Create.builder()
                .employeeId(3L)
                .type(RequestType.VACATION)
                .requestData("{\"dateFrom\": \"2026-10-01\", \"dateTo\": \"2026-10-14\"}")
                .build();
        String createJson = objectMapper.writeValueAsString(reqCreate);
        RequestDto.Create desCreate = objectMapper.readValue(createJson, RequestDto.Create.class);
        assertThat(desCreate.getType()).isEqualTo(RequestType.VACATION);
        assertThat(desCreate.getRequestData()).isEqualTo("{\"dateFrom\": \"2026-10-01\", \"dateTo\": \"2026-10-14\"}");

        RequestDto.Process reqProcess = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .processedById(1L)
                .resolutionComment("Одобрено руководителем")
                .build();
        String processJson = objectMapper.writeValueAsString(reqProcess);
        RequestDto.Process desProcess = objectMapper.readValue(processJson, RequestDto.Process.class);
        assertThat(desProcess.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(desProcess.getResolutionComment()).isEqualTo("Одобрено руководителем");
    }

    @Test
    @DisplayName("SliceResponse generic serialization and deserialization")
    void testSliceResponseSerialization() throws JsonProcessingException {
        CompanyDto.Response company = CompanyDto.Response.builder()
                .id(1L)
                .name("ООО Ромашка")
                .build();

        SliceResponse<CompanyDto.Response> slice = new SliceResponse<>(
                List.of(company),
                0,
                10,
                false
        );

        String json = objectMapper.writeValueAsString(slice);
        assertThat(json).contains("\"pageNumber\":0");
        assertThat(json).contains("\"hasNext\":false");
        assertThat(json).contains("\"ООО Ромашка\"");
    }

    @Test
    @DisplayName("Kafka Event DTOs and NotificationDto/FileMetadataDto serialization and deserialization")
    void testKafkaEventDtosSerialization() throws JsonProcessingException {
        Instant now = Instant.parse("2026-10-23T12:00:00Z");

        portal.dto.event.EmployeeEventDto empEvent = portal.dto.event.EmployeeEventDto.builder()
                .eventType(portal.dto.event.EmployeeEventDto.EventType.EMPLOYEE_CREATED)
                .employeeId(10L)
                .fullName("Тестовый Сотрудник")
                .phone("+79998887766")
                .status("ACTIVE")
                .branchId(1L)
                .positionId(2L)
                .timestamp(now)
                .build();
        String empJson = objectMapper.writeValueAsString(empEvent);
        assertThat(empJson).contains("\"eventType\":\"EMPLOYEE_CREATED\"");
        portal.dto.event.EmployeeEventDto desEmp = objectMapper.readValue(empJson, portal.dto.event.EmployeeEventDto.class);
        assertThat(desEmp.getFullName()).isEqualTo("Тестовый Сотрудник");

        portal.dto.event.AttendanceEventDto attEvent = portal.dto.event.AttendanceEventDto.builder()
                .eventType(portal.dto.event.AttendanceEventDto.EventType.LATE_ARRIVAL)
                .recordId(50L)
                .employeeId(10L)
                .shiftId(100L)
                .lateMinutes(15)
                .overtimeMinutes(0)
                .timestamp(now)
                .build();
        String attJson = objectMapper.writeValueAsString(attEvent);
        assertThat(attJson).contains("\"lateMinutes\":15");
        portal.dto.event.AttendanceEventDto desAtt = objectMapper.readValue(attJson, portal.dto.event.AttendanceEventDto.class);
        assertThat(desAtt.getLateMinutes()).isEqualTo(15);

        portal.dto.event.ScheduleEventDto schedEvent = portal.dto.event.ScheduleEventDto.builder()
                .eventType(portal.dto.event.ScheduleEventDto.EventType.SHIFT_ASSIGNED)
                .scheduleId(1L)
                .shiftId(100L)
                .employeeId(10L)
                .shiftDate(LocalDate.of(2026, 11, 1))
                .timestamp(now)
                .build();
        String schedJson = objectMapper.writeValueAsString(schedEvent);
        portal.dto.event.ScheduleEventDto desSched = objectMapper.readValue(schedJson, portal.dto.event.ScheduleEventDto.class);
        assertThat(desSched.getShiftDate()).isEqualTo(LocalDate.of(2026, 11, 1));

        portal.dto.event.NotificationEventDto notifEvent = portal.dto.event.NotificationEventDto.builder()
                .eventId("evt-123")
                .recipientUserId(2L)
                .title("Заявка одобрена")
                .message("Ваш отпуск утвержден")
                .type("SUCCESS")
                .priority(portal.dto.event.NotificationEventDto.Priority.HIGH)
                .timestamp(now)
                .build();
        String notifJson = objectMapper.writeValueAsString(notifEvent);
        portal.dto.event.NotificationEventDto desNotif = objectMapper.readValue(notifJson, portal.dto.event.NotificationEventDto.class);
        assertThat(desNotif.getTitle()).isEqualTo("Заявка одобрена");

        portal.dto.event.FileEventDto fileEvent = portal.dto.event.FileEventDto.builder()
                .eventType(portal.dto.event.FileEventDto.EventType.FILE_UPLOADED)
                .fileId(101L)
                .filename("doc.pdf")
                .originalFilename("Заявление.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .uploadedBy("ivanov")
                .timestamp(now)
                .build();
        String fileJson = objectMapper.writeValueAsString(fileEvent);
        portal.dto.event.FileEventDto desFile = objectMapper.readValue(fileJson, portal.dto.event.FileEventDto.class);
        assertThat(desFile.getOriginalFilename()).isEqualTo("Заявление.pdf");

        NotificationDto notifDto = NotificationDto.builder()
                .id(1L)
                .recipientUserId(2L)
                .title("Заголовок")
                .message("Текст")
                .type("INFO")
                .link("/link")
                .isRead(false)
                .build();
        String notifDtoJson = objectMapper.writeValueAsString(notifDto);
        NotificationDto desNotifDto = objectMapper.readValue(notifDtoJson, NotificationDto.class);
        assertThat(desNotifDto.getTitle()).isEqualTo("Заголовок");

        FileMetadataDto metaDto = FileMetadataDto.builder()
                .id(1L)
                .filename("f.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(5000L)
                .downloadUrl("/api/files/1/download")
                .uploadedBy("admin")
                .build();
        String metaJson = objectMapper.writeValueAsString(metaDto);
        FileMetadataDto desMeta = objectMapper.readValue(metaJson, FileMetadataDto.class);
        assertThat(desMeta.getDownloadUrl()).isEqualTo("/api/files/1/download");
    }
}

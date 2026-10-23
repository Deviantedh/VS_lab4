package portal.attendance.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import portal.attendance.exception.GlobalExceptionHandler;
import portal.attendance.exception.ResourceNotFoundException;
import portal.attendance.service.ReactiveAttendanceService;
import portal.dto.AttendanceRecordDto;
import portal.dto.SliceResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveAttendanceControllerTest {

    @Mock
    private ReactiveAttendanceService service;

    @InjectMocks
    private ReactiveAttendanceController controller;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/attendance should return reactive SliceResponse JSON")
    void testGetAll() {
        AttendanceRecordDto.Response resp = AttendanceRecordDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .plannedStart(Instant.parse("2026-10-01T09:00:00Z"))
                .plannedEnd(Instant.parse("2026-10-01T18:00:00Z"))
                .build();

        SliceResponse<AttendanceRecordDto.Response> slice = new SliceResponse<>(
                List.of(resp),
                0,
                20,
                false
        );

        when(service.getSlice(null, 0, 20)).thenReturn(Mono.just(slice));

        webTestClient.get()
                .uri("/api/attendance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pageNumber").isEqualTo(0)
                .jsonPath("$.content[0].id").isEqualTo(1)
                .jsonPath("$.content[0].employeeId").isEqualTo(10);
    }

    @Test
    @DisplayName("POST /api/attendance/check-in should return 201 CREATED")
    void testCheckIn() {
        AttendanceRecordDto.CheckInRequest request = AttendanceRecordDto.CheckInRequest.builder()
                .employeeId(5L)
                .shiftId(100L)
                .comment("Пришел вовремя")
                .build();

        AttendanceRecordDto.Response response = AttendanceRecordDto.Response.builder()
                .id(55L)
                .employeeId(5L)
                .shiftId(100L)
                .comment("Пришел вовремя")
                .build();

        when(service.checkIn(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/attendance/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(55)
                .jsonPath("$.employeeId").isEqualTo(5);
    }

    @Test
    @DisplayName("POST /api/attendance/{id}/check-out should return updated attendance record")
    void testCheckOut() {
        AttendanceRecordDto.CheckOutRequest request = AttendanceRecordDto.CheckOutRequest.builder()
                .breakMinutes(45)
                .comment("Ушел")
                .build();

        AttendanceRecordDto.Response response = AttendanceRecordDto.Response.builder()
                .id(55L)
                .employeeId(5L)
                .breakMinutes(45)
                .comment("Ушел")
                .build();

        when(service.checkOut(eq(55L), any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/attendance/55/check-out")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(55)
                .jsonPath("$.breakMinutes").isEqualTo(45);
    }

    @Test
    @DisplayName("POST /api/attendance/{id}/check-out returns 404 NOT_FOUND handled by GlobalExceptionHandler")
    void testCheckOutNotFoundHandledByGlobalExceptionHandler() {
        AttendanceRecordDto.CheckOutRequest request = AttendanceRecordDto.CheckOutRequest.builder()
                .breakMinutes(45)
                .build();

        when(service.checkOut(eq(999L), any()))
                .thenReturn(Mono.error(new ResourceNotFoundException("Запись о явке с ID 999 не найдена!")));

        webTestClient.post()
                .uri("/api/attendance/999/check-out")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Запись о явке с ID 999 не найдена!");
    }

    @Test
    @DisplayName("GET /api/attendance/stream returns streaming Flux NDJSON")
    void testStreamEndpoint() {
        AttendanceRecordDto.Response resp = AttendanceRecordDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .build();

        when(service.getStream(null, 20)).thenReturn(reactor.core.publisher.Flux.just(resp));

        webTestClient.get()
                .uri("/api/attendance/stream")
                .accept(MediaType.APPLICATION_NDJSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
                .expectBodyList(AttendanceRecordDto.Response.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("GET /api/attendance/batch returns batch Flux")
    void testBatchEndpoint() {
        AttendanceRecordDto.Response resp1 = AttendanceRecordDto.Response.builder().id(1L).build();
        AttendanceRecordDto.Response resp2 = AttendanceRecordDto.Response.builder().id(2L).build();

        when(service.getByIds(List.of(1L, 2L))).thenReturn(reactor.core.publisher.Flux.just(resp1, resp2));

        webTestClient.get()
                .uri("/api/attendance/batch?ids=1,2")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AttendanceRecordDto.Response.class)
                .hasSize(2);
    }
}

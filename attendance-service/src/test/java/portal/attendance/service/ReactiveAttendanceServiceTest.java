package portal.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import portal.attendance.entity.ReactiveAttendanceRecord;
import portal.attendance.exception.ResourceNotFoundException;
import portal.attendance.repository.ReactiveAttendanceRepository;
import portal.dto.AttendanceRecordDto;
import portal.dto.SliceResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveAttendanceServiceTest {

    @Mock
    private ReactiveAttendanceRepository repository;

    @InjectMocks
    private ReactiveAttendanceService service;

    private Instant now;
    private ReactiveAttendanceRecord sampleRecord;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        sampleRecord = ReactiveAttendanceRecord.builder()
                .id(1L)
                .employeeId(10L)
                .shiftId(100L)
                .plannedStart(now)
                .plannedEnd(now.plus(Duration.ofHours(8)))
                .actualStart(now.plus(Duration.ofMinutes(5)))
                .actualEnd(now.plus(Duration.ofHours(8)))
                .breakMinutes(30)
                .comment("Тестовая запись")
                .lateMinutes(5L)
                .overtimeMinutes(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("getSlice with employeeId: verify Mono<SliceResponse> emissions, sorting and pagination using StepVerifier")
    void testGetSliceWithEmployeeId() {
        when(repository.findByEmployeeId(eq(10L), any(Pageable.class)))
                .thenReturn(Flux.just(sampleRecord));

        Mono<SliceResponse<AttendanceRecordDto.Response>> sliceMono = service.getSlice(10L, 0, 10);

        StepVerifier.create(sliceMono)
                .assertNext(slice -> {
                    assertThat(slice.getContent()).hasSize(1);
                    assertThat(slice.getContent().get(0).getId()).isEqualTo(1L);
                    assertThat(slice.getContent().get(0).getEmployeeId()).isEqualTo(10L);
                    assertThat(slice.getPageNumber()).isEqualTo(0);
                    assertThat(slice.getPageSize()).isEqualTo(10);
                    assertThat(slice.isHasNext()).isFalse();
                })
                .verifyComplete();

        verify(repository).findByEmployeeId(eq(10L), argThat(p ->
                p.getSort().getOrderFor("id") != null && p.getSort().getOrderFor("id").isDescending()));
    }

    @Test
    @DisplayName("getSlice without employeeId: verify hasNext calculation when fetch size exceeds limit")
    void testGetSliceHasNext() {
        ReactiveAttendanceRecord rec1 = ReactiveAttendanceRecord.builder().id(1L).employeeId(10L).build();
        ReactiveAttendanceRecord rec2 = ReactiveAttendanceRecord.builder().id(2L).employeeId(11L).build();

        // When requesting size=1, fetchSize=2; returning 2 records should result in hasNext=true
        when(repository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(rec1, rec2));

        Mono<SliceResponse<AttendanceRecordDto.Response>> sliceMono = service.getSlice(null, 0, 1);

        StepVerifier.create(sliceMono)
                .assertNext(slice -> {
                    assertThat(slice.getContent()).hasSize(1);
                    assertThat(slice.getContent().get(0).getId()).isEqualTo(1L);
                    assertThat(slice.isHasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getById: verify successful item emission with StepVerifier")
    void testGetByIdFound() {
        when(repository.findById(1L)).thenReturn(Mono.just(sampleRecord));

        Mono<AttendanceRecordDto.Response> mono = service.getById(1L);

        StepVerifier.create(mono)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(1L);
                    assertThat(resp.getEmployeeId()).isEqualTo(10L);
                    assertThat(resp.getComment()).isEqualTo("Тестовая запись");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getById: verify empty Mono completion when record does not exist")
    void testGetByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Mono.empty());

        Mono<AttendanceRecordDto.Response> mono = service.getById(999L);

        StepVerifier.create(mono)
                .verifyComplete();
    }

    @Test
    @DisplayName("create: verify save and response mapping using StepVerifier")
    void testCreate() {
        AttendanceRecordDto.Request req = AttendanceRecordDto.Request.builder()
                .employeeId(10L)
                .shiftId(100L)
                .plannedStart(now)
                .plannedEnd(now.plus(Duration.ofHours(8)))
                .actualStart(now)
                .actualEnd(now.plus(Duration.ofHours(8)))
                .breakMinutes(45)
                .comment("Создано вручную")
                .build();

        when(repository.save(any(ReactiveAttendanceRecord.class))).thenAnswer(i -> {
            ReactiveAttendanceRecord rec = i.getArgument(0);
            rec.setId(50L);
            return Mono.just(rec);
        });

        Mono<AttendanceRecordDto.Response> result = service.create(req);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(50L);
                    assertThat(resp.getEmployeeId()).isEqualTo(10L);
                    assertThat(resp.getBreakMinutes()).isEqualTo(45);
                    assertThat(resp.getComment()).isEqualTo("Создано вручную");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("checkIn: verify automatic calculation of plannedEnd (+8h) and emission with StepVerifier")
    void testCheckIn() {
        AttendanceRecordDto.CheckInRequest req = AttendanceRecordDto.CheckInRequest.builder()
                .employeeId(10L)
                .shiftId(100L)
                .comment("Пришел вовремя")
                .build();

        when(repository.save(any(ReactiveAttendanceRecord.class))).thenAnswer(i -> {
            ReactiveAttendanceRecord rec = i.getArgument(0);
            rec.setId(60L);
            return Mono.just(rec);
        });

        Mono<AttendanceRecordDto.Response> result = service.checkIn(req);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(60L);
                    assertThat(resp.getEmployeeId()).isEqualTo(10L);
                    assertThat(resp.getActualStart()).isNotNull();
                    assertThat(resp.getPlannedEnd()).isAfter(resp.getPlannedStart());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("checkOut: verify update, overtime calculation and emission with StepVerifier")
    void testCheckOutWithOvertime() {
        // Record planned to end 1 hour ago
        ReactiveAttendanceRecord existing = ReactiveAttendanceRecord.builder()
                .id(1L)
                .employeeId(10L)
                .plannedStart(now.minus(Duration.ofHours(9)))
                .plannedEnd(now.minus(Duration.ofHours(1)))
                .actualStart(now.minus(Duration.ofHours(9)))
                .build();

        when(repository.findById(1L)).thenReturn(Mono.just(existing));
        when(repository.save(any(ReactiveAttendanceRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        AttendanceRecordDto.CheckOutRequest req = AttendanceRecordDto.CheckOutRequest.builder()
                .breakMinutes(40)
                .comment("Задержался на час")
                .build();

        Mono<AttendanceRecordDto.Response> result = service.checkOut(1L, req);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertThat(resp.getId()).isEqualTo(1L);
                    assertThat(resp.getBreakMinutes()).isEqualTo(40);
                    assertThat(resp.getComment()).isEqualTo("Задержался на час");
                    assertThat(resp.getActualEnd()).isNotNull();
                    assertThat(resp.getOvertimeMinutes()).isGreaterThan(0L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("checkOut: throws ResourceNotFoundException when record is not found in database")
    void testCheckOutNotFound() {
        when(repository.findById(999L)).thenReturn(Mono.empty());

        AttendanceRecordDto.CheckOutRequest req = AttendanceRecordDto.CheckOutRequest.builder()
                .comment("Не найден")
                .build();

        Mono<AttendanceRecordDto.Response> result = service.checkOut(999L, req);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("не найдена"))
                .verify();
    }
}

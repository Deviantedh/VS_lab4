package portal.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import portal.attendance.service.ReactiveAttendanceService;
import portal.dto.AttendanceRecordDto;
import portal.dto.SliceResponse;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance (Reactive R2DBC)", description = "Учет фактического времени (Spring WebFlux + R2DBC)")
public class ReactiveAttendanceController {

    private final ReactiveAttendanceService service;

    @GetMapping
    @Operation(summary = "Получить записи явок для бесконечной ленты (Infinite Scroll / Slice, реактивно)")
    public Mono<SliceResponse<AttendanceRecordDto.Response>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.getSlice(employeeId, page, size);
    }

    @GetMapping(value = "/stream", produces = {org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE, org.springframework.http.MediaType.APPLICATION_NDJSON_VALUE})
    @Operation(summary = "Потоковая передача явок (Flux SSE / ndjson стриминг)")
    public reactor.core.publisher.Flux<AttendanceRecordDto.Response> stream(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return service.getStream(employeeId, limit);
    }

    @GetMapping("/batch")
    @Operation(summary = "Получить пачку отметок явки по списку ID (Flux)")
    public reactor.core.publisher.Flux<AttendanceRecordDto.Response> getByIds(
            @RequestParam java.util.List<Long> ids) {
        return service.getByIds(ids);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Получить явку конкретного сотрудника для бесконечной ленты")
    public Mono<SliceResponse<AttendanceRecordDto.Response>> getByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.getSlice(employeeId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить отметку явки по ID")
    public Mono<AttendanceRecordDto.Response> getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать отметку явки вручную")
    public Mono<AttendanceRecordDto.Response> create(@Valid @RequestBody AttendanceRecordDto.Request request) {
        return service.create(request);
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Отметка о приходе на смену (Check-In) прямо сейчас")
    public Mono<AttendanceRecordDto.Response> checkIn(@Valid @RequestBody AttendanceRecordDto.CheckInRequest request) {
        return service.checkIn(request);
    }

    @PostMapping("/{id}/check-out")
    @Operation(summary = "Отметка об уходе со смены (Check-Out) с расчетом опозданий и переработок")
    public Mono<AttendanceRecordDto.Response> checkOut(
            @PathVariable Long id,
            @RequestBody(required = false) AttendanceRecordDto.CheckOutRequest request) {
        return service.checkOut(id, request != null ? request : new AttendanceRecordDto.CheckOutRequest());
    }
}

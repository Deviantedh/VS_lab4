package portal.attendance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import portal.attendance.entity.ReactiveAttendanceRecord;
import portal.attendance.exception.ResourceNotFoundException;
import portal.attendance.repository.ReactiveAttendanceRepository;
import portal.dto.AttendanceRecordDto;
import portal.dto.SliceResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ReactiveAttendanceService {

    private final ReactiveAttendanceRepository repository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<SliceResponse<AttendanceRecordDto.Response>> getSlice(Long employeeId, int page, int size) {
        int limit = Math.min(Math.max(size, 1), 50);
        int fetchSize = limit + 1; // +1 to check for hasNext

        // Явная сортировка: без неё порядок страниц не гарантирован (дубли/пропуски)
        Pageable pageable = PageRequest.of(page, fetchSize, Sort.by(Sort.Direction.DESC, "id"));

        Flux<ReactiveAttendanceRecord> source = (employeeId != null)
                ? repository.findByEmployeeId(employeeId, pageable)
                : repository.findAllBy(pageable);

        return source.map(this::toResponseDto)
                .collectList()
                .map(list -> {
                    boolean hasNext = list.size() > limit;
                    var content = hasNext ? list.subList(0, limit) : list;
                    return SliceResponse.of(content, page, limit, hasNext);
                });
    }

    public Flux<AttendanceRecordDto.Response> getStream(Long employeeId, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, boundedLimit, Sort.by(Sort.Direction.DESC, "id"));
        Flux<ReactiveAttendanceRecord> source = (employeeId != null)
                ? repository.findByEmployeeId(employeeId, pageable)
                : repository.findAllBy(pageable);
        return source.map(this::toResponseDto);
    }

    public Mono<AttendanceRecordDto.Response> getById(Long id) {
        return repository.findById(id)
                .map(this::toResponseDto);
    }

    public Flux<AttendanceRecordDto.Response> getByIds(Iterable<Long> ids) {
        if (ids == null) {
            return Flux.empty();
        }
        return repository.findAllById(ids)
                .map(this::toResponseDto);
    }

    public Mono<AttendanceRecordDto.Response> create(AttendanceRecordDto.Request request) {
        Instant now = Instant.now();
        ReactiveAttendanceRecord record = ReactiveAttendanceRecord.builder()
                .employeeId(request.getEmployeeId())
                .shiftId(request.getShiftId())
                .plannedStart(request.getPlannedStart())
                .plannedEnd(request.getPlannedEnd())
                .actualStart(request.getActualStart())
                .actualEnd(request.getActualEnd())
                .breakMinutes(request.getBreakMinutes())
                .comment(request.getComment())
                .lateMinutes(0L)
                .overtimeMinutes(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return repository.save(record).map(this::toResponseDto);
    }

    public Mono<AttendanceRecordDto.Response> checkIn(AttendanceRecordDto.CheckInRequest request) {
        Instant now = Instant.now();
        Instant plannedEnd = now.plus(Duration.ofHours(8));

        ReactiveAttendanceRecord record = ReactiveAttendanceRecord.builder()
                .employeeId(request.getEmployeeId())
                .shiftId(request.getShiftId())
                .plannedStart(now)
                .plannedEnd(plannedEnd)
                .actualStart(now)
                .comment(request.getComment())
                .lateMinutes(0L)
                .overtimeMinutes(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return repository.save(record)
                .doOnNext(saved -> publishAttendanceEvent(portal.dto.event.AttendanceEventDto.EventType.CHECK_IN, saved))
                .map(this::toResponseDto);
    }

    public Mono<AttendanceRecordDto.Response> checkOut(Long id, AttendanceRecordDto.CheckOutRequest request) {
        Instant now = Instant.now();
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Запись о явке с ID " + id + " не найдена!")))
                .flatMap(record -> {
                    record.setActualEnd(now);
                    if (request.getBreakMinutes() != null) {
                        record.setBreakMinutes(request.getBreakMinutes());
                    }
                    if (request.getComment() != null) {
                        record.setComment(request.getComment());
                    }
                    if (record.getPlannedEnd() != null && now.isAfter(record.getPlannedEnd())) {
                        record.setOvertimeMinutes(Duration.between(record.getPlannedEnd(), now).toMinutes());
                    }
                    record.setUpdatedAt(now);
                    return repository.save(record);
                })
                .doOnNext(saved -> {
                    portal.dto.event.AttendanceEventDto.EventType type = (saved.getOvertimeMinutes() != null && saved.getOvertimeMinutes() > 0)
                            ? portal.dto.event.AttendanceEventDto.EventType.OVERTIME
                            : portal.dto.event.AttendanceEventDto.EventType.CHECK_OUT;
                    publishAttendanceEvent(type, saved);
                })
                .map(this::toResponseDto);
    }

    private void publishAttendanceEvent(portal.dto.event.AttendanceEventDto.EventType eventType, ReactiveAttendanceRecord record) {
        if (kafkaTemplate != null && record != null) {
            try {
                portal.dto.event.AttendanceEventDto event = portal.dto.event.AttendanceEventDto.builder()
                        .eventType(eventType)
                        .recordId(record.getId())
                        .employeeId(record.getEmployeeId())
                        .shiftId(record.getShiftId())
                        .actualStart(record.getActualStart() != null ? java.time.LocalDateTime.ofInstant(record.getActualStart(), java.time.ZoneId.systemDefault()) : null)
                        .actualEnd(record.getActualEnd() != null ? java.time.LocalDateTime.ofInstant(record.getActualEnd(), java.time.ZoneId.systemDefault()) : null)
                        .lateMinutes(record.getLateMinutes() != null ? record.getLateMinutes().intValue() : 0)
                        .overtimeMinutes(record.getOvertimeMinutes() != null ? record.getOvertimeMinutes().intValue() : 0)
                        .timestamp(Instant.now())
                        .build();
                kafkaTemplate.send("attendance-events", String.valueOf(record.getEmployeeId()), event);
            } catch (Exception e) {
                log.warn("Не удалось отправить событие явки в Kafka: {}", e.getMessage());
            }
        }
    }

    private AttendanceRecordDto.Response toResponseDto(ReactiveAttendanceRecord entity) {
        return AttendanceRecordDto.Response.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .shiftId(entity.getShiftId())
                .plannedStart(entity.getPlannedStart())
                .plannedEnd(entity.getPlannedEnd())
                .actualStart(entity.getActualStart())
                .actualEnd(entity.getActualEnd())
                .breakMinutes(entity.getBreakMinutes())
                .comment(entity.getComment())
                .lateMinutes(entity.getLateMinutes() != null ? entity.getLateMinutes() : 0L)
                .overtimeMinutes(entity.getOvertimeMinutes() != null ? entity.getOvertimeMinutes() : 0L)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

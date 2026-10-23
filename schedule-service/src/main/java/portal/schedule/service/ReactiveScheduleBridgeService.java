package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.ShiftDto;
import portal.entity.Shift;
import portal.schedule.client.EmployeeClient;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.ShiftRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactiveScheduleBridgeService {

    private final ShiftRepository shiftRepository;
    private final ShiftService shiftService;
    private final EmployeeClient employeeClient;

    // Реактивная обертка вокруг JPA через Schedulers.boundedElastic()
    public Mono<Page<ShiftDto.Response>> getShiftsReactive(LocalDate from, LocalDate to, int page, int size) {
        return Mono.fromCallable(() -> {
            var pageable = PageRequest.of(page, Math.min(size, 50));
            Page<Shift> shiftsPage;
            if (from != null && to != null) {
                shiftsPage = shiftRepository.findByDateBetween(from, to, pageable);
            } else {
                shiftsPage = shiftRepository.findAll(pageable);
            }
            return shiftsPage.map(this::toDto);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ShiftDto.Response> getShiftByIdReactive(Long id) {
        return Mono.fromCallable(() -> {
            Shift shift = shiftRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Смена с ID " + id + " не найдена"));
            return toDto(shift);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ShiftDto.Response> assignEmployeeToShiftReactive(Long shiftId, Long employeeId) {
        return Mono.fromCallable(() -> {
            // 1. Межсервисный вызов через Feign Client с Circuit Breaker:
            // проверяем, что сотрудник существует и доступен в employee-service
            var employee = employeeClient.getEmployeeById(employeeId);
            if (employee == null) {
                throw new ResourceNotFoundException("Сотрудник с ID " + employeeId + " не найден");
            }

            // 2. Блокирующий JPA-сценарий в транзакции: назначение со всеми
            // проверками (отпуска, пересечения смен) и записью в аудит-лог
            return shiftService.assignEmployee(shiftId, employeeId, null);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ShiftDto.Response toDto(Shift shift) {
        return ShiftDto.Response.builder()
                .id(shift.getId())
                .scheduleId(shift.getSchedule() != null ? shift.getSchedule().getId() : null)
                .date(shift.getDate())
                .timeFrom(shift.getTimeFrom())
                .timeTo(shift.getTimeTo())
                .breakMinutes(shift.getBreakMinutes())
                .assignedEmployees(List.of())
                .build();
    }
}

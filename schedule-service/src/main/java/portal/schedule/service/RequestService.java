package portal.schedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.AbsenceType;
import portal.dto.RequestDto;
import portal.dto.RequestStatus;
import portal.dto.RequestType;
import portal.dto.ShiftAction;
import portal.entity.*;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAbsenceRepository;
import portal.schedule.repository.RequestRepository;
import portal.schedule.repository.ShiftEmployeeLogRepository;
import portal.schedule.repository.ShiftRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final EmployeeAbsenceRepository absenceRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftEmployeeLogRepository shiftLogRepository;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public RequestService(RequestRepository requestRepository,
                          EmployeeAbsenceRepository absenceRepository,
                          ShiftRepository shiftRepository,
                          ShiftEmployeeLogRepository shiftLogRepository,
                          EmployeeService employeeService,
                          ObjectMapper objectMapper) {
        this(requestRepository, absenceRepository, shiftRepository, shiftLogRepository, employeeService, objectMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RequestService(RequestRepository requestRepository,
                          EmployeeAbsenceRepository absenceRepository,
                          ShiftRepository shiftRepository,
                          ShiftEmployeeLogRepository shiftLogRepository,
                          EmployeeService employeeService,
                          ObjectMapper objectMapper,
                          org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.requestRepository = requestRepository;
        this.absenceRepository = absenceRepository;
        this.shiftRepository = shiftRepository;
        this.shiftLogRepository = shiftLogRepository;
        this.employeeService = employeeService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(readOnly = true)
    public Page<RequestDto.Response> getAllPaged(RequestStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepository.findAllByStatus(status, pageable).map(this::toResponse);
        }
        return requestRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<RequestDto.Response> getByEmployee(Long employeeId) {
        employeeService.findEmployeeById(employeeId);
        return requestRepository.findAllByEmployeeId(employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDto.Response getById(Long id) {
        return toResponse(findRequestById(id));
    }

    @Transactional
    public RequestDto.Response create(RequestDto.Create createDto) {
        Employee employee = employeeService.findEmployeeById(createDto.getEmployeeId());

        Request request = Request.builder()
                .employee(employee)
                .type(createDto.getType())
                .status(RequestStatus.PENDING)
                .requestData(createDto.getRequestData())
                .build();

        Request saved = requestRepository.save(request);
        publishRequestEvent(portal.dto.event.ScheduleEventDto.EventType.REQUEST_CREATED, saved);
        return toResponse(saved);
    }

    /**
     * ТРАНЗАКЦИОННЫЙ СЦЕНАРИЙ №2:
     * Одобрение/отклонение заявки HR-ом или менеджером.
     * Если одобряется заявка на отпуск или отгул:
     * 1) Статус заявки переводится в APPROVED;
     * 2) В таблице employee_absences автоматически фиксируется запись об отсутствии;
     * 3) Сотрудник автоматически снимается со всех плановых смен, попадающих на даты отпуска, с фиксацией в аудит-логе.
     */
    @Transactional
    public RequestDto.Response processRequest(Long id, RequestDto.Process processDto) {
        Request request = findRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessConflictException("Заявка с ID " + id + " уже была обработана ранее (" + request.getStatus() + ")");
        }

        if (processDto.getStatus() != RequestStatus.APPROVED && processDto.getStatus() != RequestStatus.REJECTED) {
            throw new BusinessConflictException("Решение по заявке должно быть либо APPROVED, либо REJECTED");
        }

        Employee processedBy = null;
        if (processDto.getProcessedById() != null) {
            processedBy = employeeService.findEmployeeById(processDto.getProcessedById());
        }

        request.setStatus(processDto.getStatus());
        request.setProcessedBy(processedBy);
        request.setProcessedAt(Instant.now());
        request.setResolutionComment(processDto.getResolutionComment());

        // Если заявка ОДОБРЕНА и связана с отсутствием (отпуск / отгул)
        if (processDto.getStatus() == RequestStatus.APPROVED &&
                (request.getType() == RequestType.VACATION || request.getType() == RequestType.DAY_OFF)) {
            handleApprovedAbsence(request, processedBy);
        }

        Request saved = requestRepository.save(request);
        portal.dto.event.ScheduleEventDto.EventType eventType = (processDto.getStatus() == RequestStatus.APPROVED)
                ? portal.dto.event.ScheduleEventDto.EventType.REQUEST_APPROVED
                : portal.dto.event.ScheduleEventDto.EventType.REQUEST_REJECTED;
        publishRequestEvent(eventType, saved);
        return toResponse(saved);
    }

    private void publishRequestEvent(portal.dto.event.ScheduleEventDto.EventType eventType, Request request) {
        if (kafkaTemplate != null && request != null) {
            try {
                portal.dto.event.ScheduleEventDto event = portal.dto.event.ScheduleEventDto.builder()
                        .eventType(eventType)
                        .requestId(request.getId())
                        .employeeId(request.getEmployee() != null ? request.getEmployee().getId() : null)
                        .requestType(request.getType() != null ? request.getType().name() : null)
                        .timestamp(Instant.now())
                        .build();
                kafkaTemplate.send("schedule-events", String.valueOf(request.getId()), event);
            } catch (Exception e) {
                log.warn("Не удалось отправить событие заявки в Kafka: {}", e.getMessage());
            }
        }
    }

    private void handleApprovedAbsence(Request request, Employee processedBy) {
        if (request.getRequestData() == null || request.getRequestData().isBlank()) {
            throw new BusinessConflictException("В заявке на отсутствие отсутствуют параметры (requestData)");
        }

        LocalDate dateFrom;
        LocalDate dateTo;

        try {
            JsonNode root = objectMapper.readTree(request.getRequestData());
            if (!root.has("dateFrom") || !root.has("dateTo")) {
                throw new BusinessConflictException("В параметрах заявки отсутствуют обязательные поля dateFrom или dateTo");
            }
            dateFrom = LocalDate.parse(root.get("dateFrom").asText());
            dateTo = LocalDate.parse(root.get("dateTo").asText());
        } catch (BusinessConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessConflictException("Некорректный формат дат или JSON в параметрах заявки: " + e.getMessage());
        }

        if (dateFrom.isAfter(dateTo)) {
            throw new BusinessConflictException("Дата начала отсутствия (" + dateFrom + ") не может быть позже даты окончания (" + dateTo + ")");
        }

        // 1. Создаем запись в календаре отсутствий
        AbsenceType absenceType = request.getType() == RequestType.VACATION
                ? AbsenceType.VACATION
                : AbsenceType.UNPAID_LEAVE;

        EmployeeAbsence absence = EmployeeAbsence.builder()
                .employee(request.getEmployee())
                .request(request)
                .type(absenceType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .comment("Автоматически создано по одобренной заявке #" + request.getId())
                .build();
        absenceRepository.save(absence);

        // 2. Снимаем сотрудника со смен, пересекающихся с датами отпуска
        List<Shift> conflictingShifts = shiftRepository.findShiftsForEmployeeBetweenDates(
                request.getEmployee().getId(), dateFrom, dateTo
        );

        for (Shift shift : conflictingShifts) {
            shift.getEmployees().removeIf(e -> e.getId().equals(request.getEmployee().getId()));
            shiftRepository.save(shift);

            ShiftEmployeeLog shiftLog = ShiftEmployeeLog.builder()
                    .shift(shift)
                    .employee(request.getEmployee())
                    .action(ShiftAction.REMOVED)
                    .createdBy(processedBy)
                    .build();
            shiftLogRepository.save(shiftLog);
        }
    }

    public Request findRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка с ID " + id + " не найдена"));
    }

    public RequestDto.Response toResponse(Request r) {
        return RequestDto.Response.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeName(r.getEmployee().getName())
                .type(r.getType())
                .status(r.getStatus())
                .requestData(r.getRequestData())
                .processedById(r.getProcessedBy() != null ? r.getProcessedBy().getId() : null)
                .processedByName(r.getProcessedBy() != null ? r.getProcessedBy().getName() : null)
                .processedAt(r.getProcessedAt())
                .resolutionComment(r.getResolutionComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}

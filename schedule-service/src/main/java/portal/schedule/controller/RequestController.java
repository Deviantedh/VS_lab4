package portal.schedule.controller;
import portal.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import portal.dto.RequestDto;
import portal.dto.RequestStatus;
import portal.schedule.service.RequestService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Requests", description = "Универсальный документооборот: заявки на отпуск, отгулы, справки 2-НДФЛ, медосмотры")
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    @Operation(summary = "Получить список заявок с пагинацией и фильтрацией по статусу")
    public ResponseEntity<Page<RequestDto.Response>> getAll(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(requestService.getAllPaged(status, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заявку по ID")
    public ResponseEntity<RequestDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Получить все заявки конкретного сотрудника")
    public ResponseEntity<List<RequestDto.Response>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(requestService.getByEmployee(employeeId));
    }

    @PostMapping
    @Operation(
            summary = "Подать новую заявку (отпуск, справка 2-НДФЛ, медосмотр)",
            description = "В поле requestData передается произвольный JSON с параметрами заявки (например {\"dateFrom\": \"2026-10-01\", \"dateTo\": \"2026-10-14\"})."
    )
    public ResponseEntity<RequestDto.Response> create(@Valid @RequestBody RequestDto.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.create(request));
    }


    @PostMapping("/{id}/process")
    @Operation(
            summary = "Обработать заявку: одобрить (APPROVED) или отклонить (REJECTED) (Транзакционный сценарий №2)",
            description = "При одобрении отпуска/отгула в одной транзакции: статус переводится в APPROVED, создается запись в календаре отсутствий и сотрудник автоматически снимается со всех плановых смен на эти даты."
    )
    public ResponseEntity<RequestDto.Response> process(
            @PathVariable Long id,
            @Valid @RequestBody RequestDto.Process request
    ) {
        return ResponseEntity.ok(requestService.processRequest(id, request));
    }
}

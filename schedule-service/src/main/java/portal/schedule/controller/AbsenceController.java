package portal.schedule.controller;
import portal.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portal.dto.EmployeeAbsenceDto;
import portal.schedule.service.AbsenceService;

import java.util.List;

@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
@Tag(name = "Absences", description = "Календарь зафиксированных отсутствий сотрудников (отпуска, больничные)")
public class AbsenceController {

    private final AbsenceService absenceService;

    @GetMapping
    @Operation(summary = "Получить все записи об отсутствиях")
    public ResponseEntity<List<EmployeeAbsenceDto.Response>> getAll() {
        return ResponseEntity.ok(absenceService.getAll());
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Получить историю отсутствий конкретного сотрудника")
    public ResponseEntity<List<EmployeeAbsenceDto.Response>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(absenceService.getByEmployee(employeeId));
    }

    @PostMapping
    @Operation(summary = "Зафиксировать отсутствие вручную")
    public ResponseEntity<EmployeeAbsenceDto.Response> create(@Valid @RequestBody EmployeeAbsenceDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.create(request));
    }
}

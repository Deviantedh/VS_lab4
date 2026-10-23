package portal.employee.controller;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import portal.dto.EmployeeAssignmentDto;
import portal.dto.EmployeeDto;
import portal.dto.RoleCode;
import portal.employee.security.UserPrincipal;
import portal.employee.service.EmployeeService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Управление персоналом и назначениями на точки")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "Получить сотрудников с пагинацией (максимум 50 записей)")
    public ResponseEntity<Page<EmployeeDto.Response>> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(employeeService.getAllPaged(pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить сотрудника по ID (EMPLOYEE видит только собственную карточку)")
    public ResponseEntity<EmployeeDto.Response> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE && !id.equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Сотрудник имеет доступ только к собственной информации");
        }
        return ResponseEntity.ok(employeeService.getById(id));
    }

    public ResponseEntity<EmployeeDto.Response> getById(Long id) {
        return getById(id, null);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Зарегистрировать нового сотрудника (только HR и ADMIN)")
    public ResponseEntity<EmployeeDto.Response> create(@Valid @RequestBody EmployeeDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Обновить личные данные сотрудника (только HR и ADMIN)")
    public ResponseEntity<EmployeeDto.Response> update(@PathVariable Long id, @Valid @RequestBody EmployeeDto.Request request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Уволить сотрудника (перевод в статус DISMISSED, только HR и ADMIN)")
    public ResponseEntity<EmployeeDto.Response> dismiss(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.dismiss(id));
    }

    @PostMapping("/{id}/rehire")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Принять уволенного сотрудника обратно на работу (только HR и ADMIN)")
    public ResponseEntity<EmployeeDto.Response> rehire(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.rehire(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Удалить сотрудника из системы (только HR и ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Назначить сотрудника в филиал на должность (ADMIN, HR, MANAGER)")
    public ResponseEntity<EmployeeAssignmentDto.Response> assignToBranch(@Valid @RequestBody EmployeeAssignmentDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.assignToBranch(request));
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "История назначений сотрудника по точкам и должностям (EMPLOYEE видит только свои)")
    public ResponseEntity<List<EmployeeAssignmentDto.Response>> getAssignments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE && !id.equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Сотрудник имеет доступ только к собственной истории назначений");
        }
        return ResponseEntity.ok(employeeService.getAssignments(id));
    }

    public ResponseEntity<List<EmployeeAssignmentDto.Response>> getAssignments(Long id) {
        return getAssignments(id, null);
    }
}

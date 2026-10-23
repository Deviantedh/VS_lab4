package portal.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portal.dto.RoleCode;
import portal.dto.UserDto;
import portal.employee.security.UserPrincipal;
import portal.employee.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Учётные записи и роли пользователей (0: ADMIN, 1: HR, 2: MANAGER, 3: EMPLOYEE)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Получить список пользователей (ADMIN и HR видят всех; EMPLOYEE видит только свой профиль)")
    public ResponseEntity<List<UserDto.Response>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE) {
            return ResponseEntity.ok(List.of(userService.getById(principal.getId())));
        }
        return ResponseEntity.ok(userService.getAll());
    }

    public ResponseEntity<List<UserDto.Response>> getAll() {
        return getAll(null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID (EMPLOYEE может запросить только свой ID)")
    public ResponseEntity<UserDto.Response> getById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE && !id.equals(principal.getId())) {
            throw new AccessDeniedException("Сотрудник имеет доступ только к собственному профилю пользователя");
        }
        return ResponseEntity.ok(userService.getById(id));
    }

    public ResponseEntity<UserDto.Response> getById(Long id) {
        return getById(id, null);
    }

    @GetMapping("/by-login/{login}")
    @Operation(summary = "Получить пользователя по логину (EMPLOYEE может запросить только свой логин)")
    public ResponseEntity<UserDto.Response> getByLogin(@PathVariable String login, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE && !login.equalsIgnoreCase(principal.getUsername())) {
            throw new AccessDeniedException("Сотрудник имеет доступ только к собственному профилю пользователя");
        }
        return ResponseEntity.ok(userService.getByLogin(login));
    }

    public ResponseEntity<UserDto.Response> getByLogin(String login) {
        return getByLogin(login, null);
    }

    @GetMapping("/roles")
    @Operation(summary = "Получить справочник системных ролей (0: ADMIN, 1: HR, 2: MANAGER, 3: EMPLOYEE)")
    public ResponseEntity<List<UserDto.RoleResponse>> getRoles() {
        return ResponseEntity.ok(userService.getRoles());
    }

    @GetMapping("/by-employee/{employeeId}")
    @Operation(summary = "Получить пользователя по ID сотрудника (EMPLOYEE может запросить только свой ID)")
    public ResponseEntity<UserDto.Response> getByEmployeeId(@PathVariable Long employeeId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getRole() == RoleCode.EMPLOYEE && !employeeId.equals(principal.getEmployeeId())) {
            throw new AccessDeniedException("Сотрудник имеет доступ только к учетной записи своего сотрудника");
        }
        return ResponseEntity.ok(userService.getByEmployeeId(employeeId));
    }

    public ResponseEntity<UserDto.Response> getByEmployeeId(Long employeeId) {
        return getByEmployeeId(employeeId, null);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя (доступно только супервайзерам с ролью ADMIN)")
    public ResponseEntity<UserDto.Response> create(@Valid @RequestBody UserDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить пользователя (доступно только супервайзерам с ролью ADMIN)")
    public ResponseEntity<UserDto.Response> update(@PathVariable Long id, @Valid @RequestBody UserDto.Request request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя (доступно только супервайзерам с ролью ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

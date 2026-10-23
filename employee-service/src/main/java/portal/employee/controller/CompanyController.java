package portal.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portal.dto.CompanyDto;
import portal.employee.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Управление компаниями и сетями заведений")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "Получить список всех компаний")
    public ResponseEntity<List<CompanyDto.Response>> getAll() {
        return ResponseEntity.ok(companyService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить компанию по ID")
    public ResponseEntity<CompanyDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getById(id));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Создать новую компанию")
    public ResponseEntity<CompanyDto.Response> create(@Valid @RequestBody CompanyDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Обновить компанию")
    public ResponseEntity<CompanyDto.Response> update(@PathVariable Long id, @Valid @RequestBody CompanyDto.Request request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Удалить компанию")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

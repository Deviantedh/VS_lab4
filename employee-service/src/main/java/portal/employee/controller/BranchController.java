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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import portal.dto.BranchDto;
import portal.employee.service.BranchService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Управление филиалами и точками сети")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Получить филиалы с пагинацией (максимум 50 записей)")
    public ResponseEntity<Page<BranchDto.Response>> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(branchService.getAllPaged(pageRequest));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Получить все филиалы конкретной компании")
    public ResponseEntity<List<BranchDto.Response>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(branchService.getAllByCompany(companyId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить филиал по ID")
    public ResponseEntity<BranchDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getById(id));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Создать новый филиал")
    public ResponseEntity<BranchDto.Response> create(@Valid @RequestBody BranchDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Обновить данные филиала")
    public ResponseEntity<BranchDto.Response> update(@PathVariable Long id, @Valid @RequestBody BranchDto.Request request) {
        return ResponseEntity.ok(branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Удалить филиал")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

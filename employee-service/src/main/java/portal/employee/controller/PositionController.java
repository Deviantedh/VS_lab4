package portal.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portal.dto.PositionDto;
import portal.employee.service.PositionService;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@Tag(name = "Positions", description = "Управление справочником должностей")
public class PositionController {

    private final PositionService positionService;

    @GetMapping
    @Operation(summary = "Получить список всех должностей")
    public ResponseEntity<List<PositionDto.Response>> getAll() {
        return ResponseEntity.ok(positionService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить должность по ID")
    public ResponseEntity<PositionDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(positionService.getById(id));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Создать новую должность")
    public ResponseEntity<PositionDto.Response> create(@Valid @RequestBody PositionDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(positionService.create(request));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Обновить должность")
    public ResponseEntity<PositionDto.Response> update(@PathVariable Long id, @Valid @RequestBody PositionDto.Request request) {
        return ResponseEntity.ok(positionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Удалить должность")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package portal.schedule.controller;
import portal.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import portal.dto.ScheduleDto;
import portal.schedule.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Управление расписаниями филиалов")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Получить расписания по филиалу")
    public ResponseEntity<List<ScheduleDto.Response>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(scheduleService.getByBranch(branchId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить расписание по ID")
    public ResponseEntity<ScheduleDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Создать новое расписание для филиала")
    public ResponseEntity<ScheduleDto.Response> create(@Valid @RequestBody ScheduleDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить расписание")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

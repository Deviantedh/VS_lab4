package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.ScheduleDto;
import portal.entity.Branch;
import portal.entity.Employee;
import portal.entity.Schedule;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.ScheduleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final BranchService branchService;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<ScheduleDto.Response> getByBranch(Long branchId) {
        return scheduleRepository.findAllByBranchId(branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleDto.Response getById(Long id) {
        return toResponse(findScheduleById(id));
    }

    @Transactional
    public ScheduleDto.Response create(ScheduleDto.Request request) {
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new BusinessConflictException("Кажется, вы пытались воспользоваться задними числами: дата начала не может быть позже даты окончания расписания");
        }

        Branch branch = branchService.findBranchById(request.getBranchId());

        Employee createdBy = null;
        if (request.getCreatedById() != null) {
            createdBy = employeeService.findEmployeeById(request.getCreatedById());
        }

        Schedule schedule = Schedule.builder()
                .branch(branch)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .createdBy(createdBy)
                .build();

        return toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        Schedule schedule = findScheduleById(id);
        scheduleRepository.delete(schedule);
    }

    public Schedule findScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расписание с ID " + id + " не найдено: график пока не утверждён"));
    }

    public ScheduleDto.Response toResponse(Schedule schedule) {
        return ScheduleDto.Response.builder()
                .id(schedule.getId())
                .branchId(schedule.getBranch().getId())
                .branchName(schedule.getBranch().getName())
                .dateFrom(schedule.getDateFrom())
                .dateTo(schedule.getDateTo())
                .createdById(schedule.getCreatedBy() != null ? schedule.getCreatedBy().getId() : null)
                .createdByName(schedule.getCreatedBy() != null ? schedule.getCreatedBy().getName() : null)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}

package portal.schedule.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.AbsenceType;
import portal.dto.EmployeeAbsenceDto;
import portal.entity.Employee;
import portal.entity.EmployeeAbsence;
import portal.entity.Request;
import portal.schedule.exception.BusinessConflictException;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.EmployeeAbsenceRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private EmployeeAbsenceRepository absenceRepository;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AbsenceService absenceService;

    @Test
    @DisplayName("getAll returns list of absence responses")
    void testGetAll() {
        Employee emp = Employee.builder().id(1L).name("Иван Иванов").build();
        EmployeeAbsence absence = EmployeeAbsence.builder()
                .id(10L)
                .employee(emp)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 6, 1))
                .dateTo(LocalDate.of(2026, 6, 14))
                .comment("Летний отпуск")
                .createdAt(Instant.now())
                .build();

        when(absenceRepository.findAll()).thenReturn(List.of(absence));

        List<EmployeeAbsenceDto.Response> result = absenceService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getEmployeeName()).isEqualTo("Иван Иванов");
        assertThat(result.get(0).getType()).isEqualTo(AbsenceType.VACATION);
    }

    @Test
    @DisplayName("getByEmployee returns absences for given employee")
    void testGetByEmployee() {
        Employee emp = Employee.builder().id(1L).name("Иван Иванов").build();
        Request req = Request.builder().id(99L).build();
        EmployeeAbsence absence = EmployeeAbsence.builder()
                .id(10L)
                .employee(emp)
                .request(req)
                .type(AbsenceType.SICK_LEAVE)
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 5))
                .createdAt(Instant.now())
                .build();

        when(employeeService.findEmployeeById(1L)).thenReturn(emp);
        when(absenceRepository.findAllByEmployeeId(1L)).thenReturn(List.of(absence));

        List<EmployeeAbsenceDto.Response> result = absenceService.getByEmployee(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequestId()).isEqualTo(99L);
        assertThat(result.get(0).getType()).isEqualTo(AbsenceType.SICK_LEAVE);
    }

    @Test
    @DisplayName("create throws BusinessConflictException when dateFrom > dateTo")
    void testCreate_InvalidDates() {
        EmployeeAbsenceDto.Request req = EmployeeAbsenceDto.Request.builder()
                .employeeId(1L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 6, 15))
                .dateTo(LocalDate.of(2026, 6, 10))
                .build();

        assertThatThrownBy(() -> absenceService.create(req))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("не может быть позже даты окончания");
    }

    @Test
    @DisplayName("create successfully creates absence")
    void testCreate_Success() {
        Employee emp = Employee.builder().id(1L).name("Иван Иванов").build();
        EmployeeAbsenceDto.Request req = EmployeeAbsenceDto.Request.builder()
                .employeeId(1L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 6, 1))
                .dateTo(LocalDate.of(2026, 6, 14))
                .comment("Отпуск")
                .build();

        EmployeeAbsence saved = EmployeeAbsence.builder()
                .id(100L)
                .employee(emp)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 6, 1))
                .dateTo(LocalDate.of(2026, 6, 14))
                .comment("Отпуск")
                .createdAt(Instant.now())
                .build();

        when(employeeService.findEmployeeById(1L)).thenReturn(emp);
        when(absenceRepository.save(any(EmployeeAbsence.class))).thenReturn(saved);

        EmployeeAbsenceDto.Response resp = absenceService.create(req);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getEmployeeId()).isEqualTo(1L);
        assertThat(resp.getComment()).isEqualTo("Отпуск");
    }

    @Test
    @DisplayName("findAbsenceById throws ResourceNotFoundException when not found")
    void testFindAbsenceById_NotFound() {
        when(absenceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> absenceService.findAbsenceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999 не найдена");
    }

    @Test
    @DisplayName("findAbsenceById returns absence when found")
    void testFindAbsenceById_Found() {
        Employee emp = Employee.builder().id(1L).name("Иван Иванов").build();
        EmployeeAbsence absence = EmployeeAbsence.builder().id(10L).employee(emp).build();

        when(absenceRepository.findById(10L)).thenReturn(Optional.of(absence));

        EmployeeAbsence result = absenceService.findAbsenceById(10L);
        assertThat(result).isEqualTo(absence);
    }
}

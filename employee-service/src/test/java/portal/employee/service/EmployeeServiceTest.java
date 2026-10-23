package portal.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import portal.dto.EmployeeAssignmentDto;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.employee.exception.BusinessConflictException;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.EmployeeAssignmentRepository;
import portal.employee.repository.EmployeeRepository;
import portal.entity.Branch;
import portal.entity.Employee;
import portal.entity.EmployeeAssignment;
import portal.entity.Position;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeAssignmentRepository assignmentRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private PositionService positionService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .id(1L)
                .name("Алексей Смирнов")
                .phone("+79991234567")
                .birthDate(LocalDate.of(2000, 1, 1))
                .hireDate(LocalDate.of(2023, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getAllPaged should return paged responses")
    void testGetAllPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        when(employeeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(employee)));

        Page<EmployeeDto.Response> result = employeeService.getAllPaged(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Алексей Смирнов");
        verify(employeeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getById should return employee response when found")
    void testGetByIdFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeDto.Response response = employeeService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Алексей Смирнов");
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when not found")
    void testGetByIdNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Сотрудник с ID 99 не найден");
    }

    @Test
    @DisplayName("create should save and return employee when phone is unique")
    void testCreateSuccess() {
        EmployeeDto.Request request = EmployeeDto.Request.builder()
                .name("Дмитрий Иванов")
                .phone("+79998887766")
                .birthDate(LocalDate.of(1998, 5, 10))
                .hireDate(LocalDate.of(2024, 2, 1))
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.existsByPhone("+79998887766")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        EmployeeDto.Response created = employeeService.create(request);

        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getName()).isEqualTo("Дмитрий Иванов");
        assertThat(created.getPhone()).isEqualTo("+79998887766");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("create should throw BusinessConflictException if phone already exists")
    void testCreateConflict() {
        EmployeeDto.Request request = EmployeeDto.Request.builder()
                .name("Двойник")
                .phone("+79991234567")
                .build();

        when(employeeRepository.existsByPhone("+79991234567")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже числится в штате");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should update fields and save")
    void testUpdateSuccess() {
        EmployeeDto.Request updateRequest = EmployeeDto.Request.builder()
                .name("Алексей Обновленный")
                .phone("+79991234567")
                .birthDate(LocalDate.of(2000, 1, 1))
                .hireDate(LocalDate.of(2023, 1, 1))
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeDto.Response updated = employeeService.update(1L, updateRequest);

        assertThat(updated.getName()).isEqualTo("Алексей Обновленный");
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("update should throw BusinessConflictException when phone is taken by another employee")
    void testUpdatePhoneConflict() {
        EmployeeDto.Request updateRequest = EmployeeDto.Request.builder()
                .name("Алексей")
                .phone("+79995554433")
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByPhone("+79995554433")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.update(1L, updateRequest))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже принадлежит другому сотруднику");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("dismiss should change status to DISMISSED and set dismissalDate")
    void testDismissSuccess() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeDto.Response dismissed = employeeService.dismiss(1L);

        assertThat(dismissed.getStatus()).isEqualTo(EmployeeStatus.DISMISSED);
        assertThat(dismissed.getDismissalDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("dismiss should throw BusinessConflictException if already dismissed")
    void testDismissAlreadyDismissed() {
        employee.setStatus(EmployeeStatus.DISMISSED);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.dismiss(1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже уволен");
    }

    @Test
    @DisplayName("rehire should restore status to ACTIVE when previously dismissed")
    void testRehireSuccess() {
        employee.setStatus(EmployeeStatus.DISMISSED);
        employee.setDismissalDate(LocalDate.of(2025, 12, 1));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeDto.Response rehired = employeeService.rehire(1L);

        assertThat(rehired.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(rehired.getDismissalDate()).isNull();
    }

    @Test
    @DisplayName("rehire should throw BusinessConflictException if employee is not dismissed")
    void testRehireActiveConflict() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.rehire(1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("не является уволенным");
    }

    @Test
    @DisplayName("delete should remove employee")
    void testDelete() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.delete(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    @DisplayName("assignToBranch should create and return assignment")
    void testAssignToBranch() {
        Branch branch = Branch.builder().id(10L).name("Главный филиал").build();
        Position position = Position.builder().id(5L).title("Инженер").build();

        EmployeeAssignmentDto.Request assignReq = EmployeeAssignmentDto.Request.builder()
                .employeeId(1L)
                .branchId(10L)
                .positionId(5L)
                .startedAt(LocalDate.of(2026, 1, 1))
                .isPrimary(true)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(branchService.findBranchById(10L)).thenReturn(branch);
        when(positionService.findPositionById(5L)).thenReturn(position);
        when(assignmentRepository.save(any(EmployeeAssignment.class))).thenAnswer(i -> {
            EmployeeAssignment a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        EmployeeAssignmentDto.Response response = employeeService.assignToBranch(assignReq);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getEmployeeId()).isEqualTo(1L);
        assertThat(response.getBranchId()).isEqualTo(10L);
        assertThat(response.getPositionTitle()).isEqualTo("Инженер");
        assertThat(response.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("getAssignments should return list of employee assignments")
    void testGetAssignments() {
        Branch branch = Branch.builder().id(10L).name("Главный филиал").build();
        Position position = Position.builder().id(5L).title("Инженер").build();
        EmployeeAssignment assignment = EmployeeAssignment.builder()
                .id(100L)
                .employee(employee)
                .branch(branch)
                .position(position)
                .startedAt(LocalDate.of(2026, 1, 1))
                .isPrimary(true)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findAllByEmployeeId(1L)).thenReturn(List.of(assignment));

        List<EmployeeAssignmentDto.Response> list = employeeService.getAssignments(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(100L);
    }
}

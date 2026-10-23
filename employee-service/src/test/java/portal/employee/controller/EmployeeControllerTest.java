package portal.employee.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.EmployeeAssignmentDto;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;
import portal.employee.service.EmployeeService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    @Test
    @DisplayName("getAll returns paged employees")
    void testGetAll() {
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).name("Ivan Ivanov").status(EmployeeStatus.ACTIVE).build();
        Page<EmployeeDto.Response> page = new PageImpl<>(List.of(resp));
        when(employeeService.getAllPaged(any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<EmployeeDto.Response>> response = employeeController.getAll(0, 20, "id", Sort.Direction.ASC);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns employee by id")
    void testGetById() {
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).name("Ivan Ivanov").build();
        when(employeeService.getById(1L)).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created employee with 201")
    void testCreate() {
        EmployeeDto.Request req = EmployeeDto.Request.builder().name("Ivan Ivanov").build();
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(2L).name("Ivan Ivanov").build();
        when(employeeService.create(req)).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("update returns updated employee")
    void testUpdate() {
        EmployeeDto.Request req = EmployeeDto.Request.builder().name("Petr Petrov").build();
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).name("Petr Petrov").build();
        when(employeeService.update(eq(1L), any(EmployeeDto.Request.class))).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.update(1L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("dismiss returns dismissed employee")
    void testDismiss() {
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).status(EmployeeStatus.DISMISSED).build();
        when(employeeService.dismiss(1L)).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.dismiss(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("rehire returns rehired employee")
    void testRehire() {
        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).status(EmployeeStatus.ACTIVE).build();
        when(employeeService.rehire(1L)).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.rehire(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(employeeService).delete(1L);

        ResponseEntity<Void> response = employeeController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(employeeService).delete(1L);
    }

    @Test
    @DisplayName("assignToBranch returns 201 Created with assignment response")
    void testAssignToBranch() {
        EmployeeAssignmentDto.Request req = EmployeeAssignmentDto.Request.builder().employeeId(1L).branchId(10L).positionId(5L).build();
        EmployeeAssignmentDto.Response resp = EmployeeAssignmentDto.Response.builder().id(100L).employeeId(1L).branchId(10L).positionId(5L).build();
        when(employeeService.assignToBranch(req)).thenReturn(resp);

        ResponseEntity<EmployeeAssignmentDto.Response> response = employeeController.assignToBranch(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("getAssignments returns list of assignments")
    void testGetAssignments() {
        EmployeeAssignmentDto.Response resp = EmployeeAssignmentDto.Response.builder().id(100L).employeeId(1L).branchId(10L).positionId(5L).build();
        when(employeeService.getAssignments(1L)).thenReturn(List.of(resp));

        ResponseEntity<List<EmployeeAssignmentDto.Response>> response = employeeController.getAssignments(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) успешно получает свою карточку по ID")
    void testGetByIdAsEmployeeSuccess() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(2L)
                .employeeId(1L)
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        EmployeeDto.Response resp = EmployeeDto.Response.builder().id(1L).name("Тест").build();
        when(employeeService.getById(1L)).thenReturn(resp);

        ResponseEntity<EmployeeDto.Response> response = employeeController.getById(1L, empPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) получает 403 Forbidden при попытке запросить чужую карточку")
    void testGetByIdAsEmployeeForbidden() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(2L)
                .employeeId(1L)
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> employeeController.getById(2L, empPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственной информации");
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) успешно получает свою историю назначений")
    void testGetAssignmentsAsEmployeeSuccess() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(2L)
                .employeeId(1L)
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        EmployeeAssignmentDto.Response resp = EmployeeAssignmentDto.Response.builder().id(100L).employeeId(1L).build();
        when(employeeService.getAssignments(1L)).thenReturn(List.of(resp));

        ResponseEntity<List<EmployeeAssignmentDto.Response>> response = employeeController.getAssignments(1L, empPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) получает 403 Forbidden при попытке запросить чужую историю назначений")
    void testGetAssignmentsAsEmployeeForbidden() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(2L)
                .employeeId(1L)
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> employeeController.getAssignments(2L, empPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственной истории назначений");
    }
}

package portal.employee.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.UserDto;
import portal.employee.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("getAll returns user list")
    void testGetAll() {
        UserDto.Response resp = UserDto.Response.builder().id(1L).login("admin").build();
        when(userService.getAll()).thenReturn(List.of(resp));

        ResponseEntity<List<UserDto.Response>> response = userController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns user by id")
    void testGetById() {
        UserDto.Response resp = UserDto.Response.builder().id(1L).login("admin").build();
        when(userService.getById(1L)).thenReturn(resp);

        ResponseEntity<UserDto.Response> response = userController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("getByLogin returns user by login")
    void testGetByLogin() {
        UserDto.Response resp = UserDto.Response.builder().id(1L).login("admin").build();
        when(userService.getByLogin("admin")).thenReturn(resp);

        ResponseEntity<UserDto.Response> response = userController.getByLogin("admin");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("getRoles returns system role list")
    void testGetRoles() {
        UserDto.RoleResponse role = UserDto.RoleResponse.builder().id((short) 0).code(portal.dto.RoleCode.ADMIN).name("Admin").build();
        when(userService.getRoles()).thenReturn(List.of(role));

        ResponseEntity<List<UserDto.RoleResponse>> response = userController.getRoles();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(role);
    }

    @Test
    @DisplayName("getByEmployeeId returns user by employee id")
    void testGetByEmployeeId() {
        UserDto.Response resp = UserDto.Response.builder().id(1L).login("admin").employeeId(5L).build();
        when(userService.getByEmployeeId(5L)).thenReturn(resp);

        ResponseEntity<UserDto.Response> response = userController.getByEmployeeId(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created user with 201")
    void testCreate() {
        UserDto.Request req = UserDto.Request.builder().login("newuser").build();
        UserDto.Response resp = UserDto.Response.builder().id(2L).login("newuser").build();
        when(userService.create(req)).thenReturn(resp);

        ResponseEntity<UserDto.Response> response = userController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("update returns updated user")
    void testUpdate() {
        UserDto.Request req = UserDto.Request.builder().login("updated").build();
        UserDto.Response resp = UserDto.Response.builder().id(1L).login("updated").build();
        when(userService.update(1L, req)).thenReturn(resp);

        ResponseEntity<UserDto.Response> response = userController.update(1L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(userService).delete(1L);

        ResponseEntity<Void> response = userController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).delete(1L);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) видит только свой профиль при вызове getAll")
    void testGetAllAsEmployee() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(3L)
                .login("barista_dmitry")
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        UserDto.Response myResp = UserDto.Response.builder().id(3L).login("barista_dmitry").build();
        when(userService.getById(3L)).thenReturn(myResp);

        ResponseEntity<List<UserDto.Response>> response = userController.getAll(empPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(myResp);
        verify(userService, never()).getAll();
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) успешно получает собственный профиль по ID")
    void testGetByIdAsEmployeeSuccess() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(3L)
                .login("barista_dmitry")
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        UserDto.Response myResp = UserDto.Response.builder().id(3L).login("barista_dmitry").build();
        when(userService.getById(3L)).thenReturn(myResp);

        ResponseEntity<UserDto.Response> response = userController.getById(3L, empPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(myResp);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) получает 403 Forbidden при попытке запросить чужой профиль по ID")
    void testGetByIdAsEmployeeForbidden() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(3L)
                .login("barista_dmitry")
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userController.getById(1L, empPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственному профилю пользователя");
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) получает 403 Forbidden при попытке запросить чужой профиль по логину")
    void testGetByLoginAsEmployeeForbidden() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(3L)
                .login("barista_dmitry")
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userController.getByLogin("admin", empPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственному профилю пользователя");
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) получает 403 Forbidden при попытке запросить профиль чужого сотрудника")
    void testGetByEmployeeIdAsEmployeeForbidden() {
        portal.employee.security.UserPrincipal empPrincipal = portal.employee.security.UserPrincipal.builder()
                .id(3L)
                .employeeId(10L)
                .login("barista_dmitry")
                .role(portal.dto.RoleCode.EMPLOYEE)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userController.getByEmployeeId(99L, empPrincipal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к учетной записи своего сотрудника");
    }
}

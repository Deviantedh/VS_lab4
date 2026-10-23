package portal.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.RoleCode;
import portal.dto.UserDto;
import portal.employee.exception.BusinessConflictException;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.RoleRepository;
import portal.employee.repository.UserRepository;
import portal.entity.Employee;
import portal.entity.Role;
import portal.entity.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Role role;
    private Employee employee;
    private User user;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any())).thenReturn("hashed_secret");
        role = Role.builder().id((short) 1).code(RoleCode.HR).name("Менеджер по персоналу").build();
        employee = Employee.builder().id(10L).name("Иван Иванов").build();
        user = User.builder()
                .id(1L)
                .login("ivanov")
                .role(role)
                .employee(employee)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getAll returns list of users")
    void testGetAll() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto.Response> result = userService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLogin()).isEqualTo("ivanov");
        assertThat(result.get(0).getEmployeeName()).isEqualTo("Иван Иванов");
    }

    @Test
    @DisplayName("getById returns user when found")
    void testGetById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto.Response result = userService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLogin()).isEqualTo("ivanov");
    }

    @Test
    @DisplayName("getById throws exception when not found")
    void testGetById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("getByLogin returns user when found")
    void testGetByLogin_Found() {
        when(userRepository.findByLogin("ivanov")).thenReturn(Optional.of(user));

        UserDto.Response result = userService.getByLogin("ivanov");

        assertThat(result.getLogin()).isEqualTo("ivanov");
    }

    @Test
    @DisplayName("getByLogin throws exception when not found")
    void testGetByLogin_NotFound() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByLogin("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getByEmployeeId returns user when found")
    void testGetByEmployeeId_Found() {
        when(userRepository.findByEmployeeId(10L)).thenReturn(Optional.of(user));

        UserDto.Response result = userService.getByEmployeeId(10L);

        assertThat(result.getEmployeeId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getByEmployeeId throws exception when not found")
    void testGetByEmployeeId_NotFound() {
        when(userRepository.findByEmployeeId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmployeeId(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getRoles returns mapped roles")
    void testGetRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<UserDto.RoleResponse> roles = userService.getRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getCode()).isEqualTo(RoleCode.HR);
    }

    @Test
    @DisplayName("create throws BusinessConflictException when login exists")
    void testCreate_LoginExists() {
        UserDto.Request req = UserDto.Request.builder().login("ivanov").build();
        when(userRepository.existsByLogin("ivanov")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже занят");
    }

    @Test
    @DisplayName("create throws ResourceNotFoundException when role does not exist")
    void testCreate_RoleNotFound() {
        UserDto.Request req = UserDto.Request.builder().login("newuser").roleId((short) 99).build();
        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(roleRepository.findById((short) 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не существует");
    }

    @Test
    @DisplayName("create throws BusinessConflictException when employee already has account")
    void testCreate_EmployeeHasAccount() {
        UserDto.Request req = UserDto.Request.builder().login("newuser").roleId((short) 1).employeeId(10L).build();
        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(roleRepository.findById((short) 1)).thenReturn(Optional.of(role));
        when(employeeService.findEmployeeById(10L)).thenReturn(employee);
        when(userRepository.findByEmployeeId(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже имеет учетную запись");
    }

    @Test
    @DisplayName("create succeeds when valid")
    void testCreate_Success() {
        UserDto.Request req = UserDto.Request.builder().login("newuser").roleId((short) 1).employeeId(10L).build();
        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(roleRepository.findById((short) 1)).thenReturn(Optional.of(role));
        when(employeeService.findEmployeeById(10L)).thenReturn(employee);
        when(userRepository.findByEmployeeId(10L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto.Response created = userService.create(req);

        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getLogin()).isEqualTo("newuser");
    }

    @Test
    @DisplayName("update succeeds when valid")
    void testUpdate_Success() {
        UserDto.Request req = UserDto.Request.builder().login("ivanov_updated").roleId((short) 1).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLogin("ivanov_updated")).thenReturn(false);
        when(roleRepository.findById((short) 1)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDto.Response updated = userService.update(1L, req);

        assertThat(updated.getLogin()).isEqualTo("ivanov_updated");
    }

    @Test
    @DisplayName("delete removes user")
    void testDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }
}

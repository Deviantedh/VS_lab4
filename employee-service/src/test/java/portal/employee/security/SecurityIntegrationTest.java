package portal.employee.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.*;
import portal.employee.controller.AuthController;
import portal.employee.controller.EmployeeController;
import portal.employee.controller.UserController;
import portal.employee.repository.UserRepository;
import portal.employee.service.AuthService;
import portal.employee.service.EmployeeService;
import portal.employee.service.UserService;
import portal.entity.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration,org.springframework.boot.r2dbc.autoconfigure.R2dbcRepositoriesAutoConfiguration,org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private portal.employee.repository.RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserController userController;

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private AuthController authController;

    private Long employeeId;
    private Long otherEmployeeId;
    private Long createdUserId;

    @BeforeEach
    void setUp() {
        if (roleRepository.count() == 0) {
            roleRepository.save(portal.entity.Role.builder().id((short) 0).code(RoleCode.ADMIN).name("Администратор").build());
            roleRepository.save(portal.entity.Role.builder().id((short) 1).code(RoleCode.HR).name("HR").build());
            roleRepository.save(portal.entity.Role.builder().id((short) 2).code(RoleCode.MANAGER).name("Управляющий").build());
            roleRepository.save(portal.entity.Role.builder().id((short) 3).code(RoleCode.EMPLOYEE).name("Сотрудник").build());
        }

        EmployeeDto.Response emp1 = employeeService.create(EmployeeDto.Request.builder()
                .name("Дмитрий Бариста")
                .phone("+79990001122")
                .hireDate(LocalDate.now())
                .build());
        employeeId = emp1.getId();

        EmployeeDto.Response emp2 = employeeService.create(EmployeeDto.Request.builder()
                .name("Ольга Администратор")
                .phone("+79990003344")
                .hireDate(LocalDate.now())
                .build());
        otherEmployeeId = emp2.getId();

        UserDto.Response userResp = userService.create(UserDto.Request.builder()
                .login("dmitry_test")
                .password("secret123")
                .roleId((short) 3) // EMPLOYEE
                .employeeId(employeeId)
                .build());
        createdUserId = userResp.getId();
    }

    @Test
    @DisplayName("Требование 6: Все пароли сохраняются в базе данных строго в хэшированном виде (BCrypt)")
    void shouldStorePasswordsInHashedForm() {
        User user = userRepository.findById(createdUserId).orElseThrow();

        assertThat(user.getPasswordHash()).isNotNull();
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Требование 5: Модели ответа пользователей никогда не содержат пароль")
    void shouldNeverReturnPasswordInUserResponse() {
        UserDto.Response response = userService.getById(createdUserId);

        assertThat(response).isNotNull();
        assertThat(response.getLogin()).isEqualTo("dmitry_test");
        // UserDto.Response не имеет полей password / passwordHash вообще
    }

    @Test
    @DisplayName("Требование 1 и 2: Аутентификация через /api/auth/login возвращает валидный JWT токен")
    void shouldAuthenticateAndIssueJwtToken() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("dmitry_test", "secret123");
        ResponseEntity<AuthDto.AuthResponse> loginResponse = authController.login(request);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthDto.AuthResponse auth = loginResponse.getBody();
        assertThat(auth).isNotNull();
        assertThat(auth.getToken()).isNotBlank();
        assertThat(auth.getTokenType()).isEqualTo("Bearer");
        assertThat(auth.getRole()).isEqualTo(RoleCode.EMPLOYEE);
        assertThat(auth.getUserId()).isEqualTo(createdUserId);
        assertThat(auth.getEmployeeId()).isEqualTo(employeeId);

        // Проверяем валидность полученного JWT
        assertThat(jwtService.validateToken(auth.getToken())).isTrue();
        assertThat(jwtService.extractUsername(auth.getToken())).isEqualTo("dmitry_test");
        assertThat(jwtService.extractRole(auth.getToken())).isEqualTo(RoleCode.EMPLOYEE);
    }

    @Test
    @DisplayName("Ошибка авторизации при неверном пароле (401 Bad Credentials)")
    void shouldRejectInvalidPassword() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("dmitry_test", "wrong_pass");

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Неверный логин или пароль");
    }

    @Test
    @DisplayName("Требование 4: Логика методов меняется в зависимости от текущего пользователя (изоляция данных)")
    void shouldEnforceUserSpecificAccessControl() {
        UserPrincipal employeePrincipal = UserPrincipal.builder()
                .id(createdUserId)
                .login("dmitry_test")
                .role(RoleCode.EMPLOYEE)
                .employeeId(employeeId)
                .build();

        // 1. Сотрудник видит свой профиль по ID
        ResponseEntity<UserDto.Response> selfUser = userController.getById(createdUserId, employeePrincipal);
        assertThat(selfUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(selfUser.getBody().getLogin()).isEqualTo("dmitry_test");

        // 2. Сотрудник НЕ может просмотреть профиль другого пользователя (403 Forbidden)
        assertThatThrownBy(() -> userController.getById(999L, employeePrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственному профилю пользователя");

        // 3. Сотрудник видит свою карточку сотрудника
        ResponseEntity<EmployeeDto.Response> selfEmp = employeeController.getById(employeeId, employeePrincipal);
        assertThat(selfEmp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Сотрудник НЕ может просмотреть карточку другого сотрудника (403 Forbidden)
        assertThatThrownBy(() -> employeeController.getById(otherEmployeeId, employeePrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Сотрудник имеет доступ только к собственной информации");

        // 5. Администратор (ADMIN) может смотреть любого пользователя и любого сотрудника
        UserPrincipal adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .login("admin")
                .role(RoleCode.ADMIN)
                .build();

        ResponseEntity<UserDto.Response> adminViewUser = userController.getById(createdUserId, adminPrincipal);
        assertThat(adminViewUser.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<EmployeeDto.Response> adminViewEmp = employeeController.getById(employeeId, adminPrincipal);
        assertThat(adminViewEmp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Сотрудник (EMPLOYEE) видит только себя при запросе списка пользователей")
    void shouldFilterUserListForEmployee() {
        UserPrincipal employeePrincipal = UserPrincipal.builder()
                .id(createdUserId)
                .login("dmitry_test")
                .role(RoleCode.EMPLOYEE)
                .employeeId(employeeId)
                .build();

        ResponseEntity<List<UserDto.Response>> listResp = userController.getAll(employeePrincipal);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).hasSize(1);
        assertThat(listResp.getBody().get(0).getId()).isEqualTo(createdUserId);
    }

    @Test
    @DisplayName("Получение профиля текущего пользователя через /api/auth/me")
    void shouldReturnCurrentProfileViaAuthMe() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(createdUserId)
                .login("dmitry_test")
                .role(RoleCode.EMPLOYEE)
                .employeeId(employeeId)
                .build();

        ResponseEntity<UserDto.Response> meResponse = authController.getMe(principal);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().getLogin()).isEqualTo("dmitry_test");
        assertThat(meResponse.getBody().getRoleCode()).isEqualTo(RoleCode.EMPLOYEE);
    }
}

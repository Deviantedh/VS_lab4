package portal.employee.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import portal.dto.RoleCode;
import portal.entity.Employee;
import portal.entity.Role;
import portal.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("Создание UserPrincipal из сущности User со всеми полями")
    void shouldCreateFromEntityComplete() {
        Role role = Role.builder().id((short) 0).code(RoleCode.ADMIN).name("Администратор").build();
        Employee employee = Employee.builder().id(10L).name("Иван").build();
        User user = User.builder()
                .id(1L)
                .login("admin")
                .passwordHash("$2a$10$hash")
                .role(role)
                .employee(employee)
                .isActive(true)
                .build();

        UserPrincipal principal = UserPrincipal.fromEntity(user);

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(principal.getRole()).isEqualTo(RoleCode.ADMIN);
        assertThat(principal.getEmployeeId()).isEqualTo(10L);
        assertThat(principal.isActive()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Создание UserPrincipal из сущности User с null полями")
    void shouldCreateFromEntityWithNulls() {
        User user = User.builder()
                .id(2L)
                .login("null_user")
                .role(null)
                .employee(null)
                .isActive(null)
                .build();

        UserPrincipal principal = UserPrincipal.fromEntity(user);

        assertThat(principal.getId()).isEqualTo(2L);
        assertThat(principal.getUsername()).isEqualTo("null_user");
        assertThat(principal.getRole()).isEqualTo(RoleCode.EMPLOYEE);
        assertThat(principal.getEmployeeId()).isNull();
        assertThat(principal.isActive()).isFalse();
        assertThat(principal.isEnabled()).isFalse();
        assertThat(principal.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("Создание UserPrincipal из claims")
    void shouldCreateFromClaims() {
        UserPrincipal principal = UserPrincipal.fromClaims(5L, "hr", RoleCode.HR, 25L);

        assertThat(principal.getId()).isEqualTo(5L);
        assertThat(principal.getUsername()).isEqualTo("hr");
        assertThat(principal.getPassword()).isEmpty();
        assertThat(principal.getRole()).isEqualTo(RoleCode.HR);
        assertThat(principal.getEmployeeId()).isEqualTo(25L);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_HR");
    }
}

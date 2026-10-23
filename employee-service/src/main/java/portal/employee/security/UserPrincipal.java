package portal.employee.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import portal.dto.RoleCode;
import portal.entity.User;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String login;
    private final String password;
    private final RoleCode role;
    private final Long employeeId;
    private final Collection<? extends GrantedAuthority> authorities;
    @Builder.Default
    private final boolean active = true;

    public static UserPrincipal fromEntity(User user) {
        RoleCode roleCode = user.getRole() != null ? user.getRole().getCode() : RoleCode.EMPLOYEE;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleCode.name()));

        return UserPrincipal.builder()
                .id(user.getId())
                .login(user.getLogin())
                .password(user.getPasswordHash())
                .role(roleCode)
                .employeeId(user.getEmployee() != null ? user.getEmployee().getId() : null)
                .authorities(authorities)
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }

    public static UserPrincipal fromClaims(Long id, String login, RoleCode role, Long employeeId) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

        return UserPrincipal.builder()
                .id(id)
                .login(login)
                .password("")
                .role(role)
                .employeeId(employeeId)
                .authorities(authorities)
                .active(true)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

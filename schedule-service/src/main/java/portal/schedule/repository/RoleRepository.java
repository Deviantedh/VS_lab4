package portal.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Role;
import portal.dto.RoleCode;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByCode(RoleCode code);
}

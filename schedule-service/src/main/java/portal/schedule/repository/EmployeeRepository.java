package portal.schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Employee;
import portal.dto.EmployeeStatus;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByPhone(String phone);
    boolean existsByPhone(String phone);
    Page<Employee> findAllByStatus(EmployeeStatus status, Pageable pageable);
}

package portal.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}

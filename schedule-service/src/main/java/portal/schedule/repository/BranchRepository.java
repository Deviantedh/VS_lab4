package portal.schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Branch;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findAllByCompanyId(Long companyId);
    Page<Branch> findAllByIsActive(Boolean isActive, Pageable pageable);
}

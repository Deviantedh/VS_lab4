package portal.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.EmployeeAssignment;

import java.util.List;

public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, Long> {
    List<EmployeeAssignment> findAllByEmployeeId(Long employeeId);
    List<EmployeeAssignment> findAllByBranchId(Long branchId);
}

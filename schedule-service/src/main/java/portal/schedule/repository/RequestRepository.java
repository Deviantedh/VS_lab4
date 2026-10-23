package portal.schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Request;
import portal.dto.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    Page<Request> findAllByStatus(RequestStatus status, Pageable pageable);
    List<Request> findAllByEmployeeId(Long employeeId);
}

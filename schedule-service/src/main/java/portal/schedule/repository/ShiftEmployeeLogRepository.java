package portal.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.ShiftEmployeeLog;

import java.util.List;

public interface ShiftEmployeeLogRepository extends JpaRepository<ShiftEmployeeLog, Long> {
    List<ShiftEmployeeLog> findAllByShiftIdOrderByCreatedAtDesc(Long shiftId);
}

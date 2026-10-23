package portal.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByBranchId(Long branchId);
/*    List<Schedule> findAllByBranchIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(
            Long branchId, LocalDate dateTo, LocalDate dateFrom
    );*/
}

package portal.schedule.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portal.entity.Shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Page<Shift> findAllByScheduleId(Long scheduleId, Pageable pageable);

    Page<Shift> findByDateBetween(LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    @Query("SELECT s FROM Shift s JOIN s.employees e " +
           "WHERE e.id = :employeeId " +
           "AND s.date = :date " +
           "AND s.timeFrom < :timeTo " +
           "AND s.timeTo > :timeFrom " +
           "AND (:excludeShiftId IS NULL OR s.id <> :excludeShiftId)")
    List<Shift> findOverlappingShiftsForEmployee(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date,
            @Param("timeFrom") LocalTime timeFrom,
            @Param("timeTo") LocalTime timeTo,
            @Param("excludeShiftId") Long excludeShiftId
    );

    @Query("SELECT s FROM Shift s JOIN s.employees e " +
           "WHERE e.id = :employeeId " +
           "AND s.date BETWEEN :dateFrom AND :dateTo")
    List<Shift> findShiftsForEmployeeBetweenDates(
            @Param("employeeId") Long employeeId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}

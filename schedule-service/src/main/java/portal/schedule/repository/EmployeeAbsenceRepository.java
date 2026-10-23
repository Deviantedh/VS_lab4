package portal.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portal.entity.EmployeeAbsence;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeAbsenceRepository extends JpaRepository<EmployeeAbsence, Long> {

    List<EmployeeAbsence> findAllByEmployeeId(Long employeeId);

    @Query("SELECT a FROM EmployeeAbsence a " +
           "WHERE a.employee.id = :employeeId " +
           "AND a.dateFrom <= :dateTo " +
           "AND a.dateTo >= :dateFrom")
    List<EmployeeAbsence> findOverlappingAbsences(
            @Param("employeeId") Long employeeId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}

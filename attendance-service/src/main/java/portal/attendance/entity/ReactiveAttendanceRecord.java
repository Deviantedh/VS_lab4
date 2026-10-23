package portal.attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactiveAttendanceRecord {

    @Id
    private Long id;

    @Column("employee_id")
    private Long employeeId;

    @Column("shift_id")
    private Long shiftId;

    @Column("planned_start")
    private Instant plannedStart;

    @Column("planned_end")
    private Instant plannedEnd;

    @Column("actual_start")
    private Instant actualStart;

    @Column("actual_end")
    private Instant actualEnd;

    @Column("break_minutes")
    private Integer breakMinutes;

    @Column("comment")
    private String comment;

    @Column("late_minutes")
    private Long lateMinutes;

    @Column("overtime_minutes")
    private Long overtimeMinutes;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}

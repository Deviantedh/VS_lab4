package portal.entity;
import portal.dto.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    @NotNull(message = "Расписание обязательно для смены")
    private Schedule schedule;

    @Column(nullable = false)
    @NotNull(message = "Дата смены обязательна")
    private LocalDate date;

    @Column(name = "time_from", nullable = false)
    @NotNull(message = "Время начала смены обязательно")
    private LocalTime timeFrom;

    @Column(name = "time_to", nullable = false)
    @NotNull(message = "Время окончания смены обязательно")
    private LocalTime timeTo;

    @Column(name = "break_minutes", nullable = false)
    @Builder.Default
    private Integer breakMinutes = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "shift_employees",
            joinColumns = @JoinColumn(name = "shift_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "shift", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShiftEmployeeLog> logs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

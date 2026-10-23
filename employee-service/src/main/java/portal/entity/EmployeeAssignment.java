package portal.entity;
import portal.dto.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employee_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Сотрудник обязателен")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    @NotNull(message = "Филиал обязателен")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    @NotNull(message = "Должность обязательна")
    private Position position;

    @Column(name = "started_at", nullable = false)
    @NotNull(message = "Дата начала назначения обязательна")
    @Builder.Default
    private LocalDate startedAt = LocalDate.now();

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

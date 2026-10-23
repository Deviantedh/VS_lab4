package portal.entity;
import portal.dto.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "ФИО сотрудника обязательно для заполнения")
    @Size(max = 255, message = "ФИО сотрудника не может быть длиннее 255 символов")
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank(message = "Номер телефона обязателен")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Номер телефона должен быть в формате +7XXXXXXXXXX")
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "hire_date", nullable = false)
    @NotNull(message = "Дата приема на работу обязательна")
    @Builder.Default
    private LocalDate hireDate = LocalDate.now();

    @Column(name = "dismissal_date")
    private LocalDate dismissalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Статус сотрудника обязателен")
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmployeeAssignment> assignments = new ArrayList<>();

    @ManyToMany(mappedBy = "employees", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Shift> shifts = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceRecord> attendanceRecords = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Request> requests = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmployeeAbsence> absences = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

package portal.entity;
import portal.dto.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @NotNull(message = "Сотрудник обязателен для создания заявки")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Тип заявки обязателен")
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Статус заявки обязателен")
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "jsonb")
    private String requestData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Employee processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "resolution_comment", columnDefinition = "TEXT")
    private String resolutionComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

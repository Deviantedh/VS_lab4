package portal.entity;
import portal.dto.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @NotNull(message = "ID роли не может быть пустым")
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    @NotNull(message = "Код роли обязателен")
    private RoleCode code;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Название роли не должно быть пустым")
    @Size(max = 50, message = "Название роли не может превышать 50 символов")
    private String name;

    @Column(length = 255)
    @Size(max = 255, message = "Описание роли не может превышать 255 символов")
    private String description;
}

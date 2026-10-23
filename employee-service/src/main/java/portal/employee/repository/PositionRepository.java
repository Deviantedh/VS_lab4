package portal.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portal.entity.Position;

import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByTitle(String title);
    boolean existsByTitle(String title);
}

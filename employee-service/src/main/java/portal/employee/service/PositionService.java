package portal.employee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.PositionDto;
import portal.entity.Position;
import portal.employee.exception.BusinessConflictException;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.PositionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<PositionDto.Response> getAll() {
        return positionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PositionDto.Response getById(Long id) {
        return toResponse(findPositionById(id));
    }

    @Transactional
    public PositionDto.Response create(PositionDto.Request request) {
        String title = request.getTitle().trim();
        if (positionRepository.existsByTitle(title)) {
            throw new BusinessConflictException("Должность '" + title + "' уже существует в штатном расписании");
        }

        Position position = Position.builder()
                .title(title)
                .build();
        return toResponse(positionRepository.save(position));
    }

    @Transactional
    public PositionDto.Response update(Long id, PositionDto.Request request) {
        Position position = findPositionById(id);
        String title = request.getTitle().trim();

        if (!position.getTitle().equalsIgnoreCase(title) && positionRepository.existsByTitle(title)) {
            throw new BusinessConflictException("Должность '" + title + "' уже занята другим определением");
        }

        position.setTitle(title);
        return toResponse(positionRepository.save(position));
    }

    @Transactional
    public void delete(Long id) {
        Position position = findPositionById(id);
        positionRepository.delete(position);
    }

    public Position findPositionById(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Должность с ID " + id + " не найдена: кажется, такую вакансию у нас ещё не открыли"));
    }

    public PositionDto.Response toResponse(Position position) {
        return PositionDto.Response.builder()
                .id(position.getId())
                .title(position.getTitle())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }
}

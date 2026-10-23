package portal.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.PositionDto;
import portal.employee.exception.BusinessConflictException;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.PositionRepository;
import portal.entity.Position;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    private PositionService positionService;
    private Position position;

    @BeforeEach
    void setUp() {
        positionService = new PositionService(positionRepository);
        position = Position.builder()
                .id(1L)
                .title("Ведущий разработчик")
                .build();
    }

    @Test
    @DisplayName("Get all positions returns mapped DTO list")
    void testGetAll() {
        when(positionRepository.findAll()).thenReturn(List.of(position));

        List<PositionDto.Response> result = positionService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Ведущий разработчик");
    }

    @Test
    @DisplayName("Get position by ID returns DTO")
    void testGetById() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        PositionDto.Response result = positionService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Ведущий разработчик");
    }

    @Test
    @DisplayName("Get non-existent position by ID throws ResourceNotFoundException")
    void testGetByIdNotFoundThrowsException() {
        when(positionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    @DisplayName("Create position with unique title succeeds")
    void testCreateSuccess() {
        PositionDto.Request request = PositionDto.Request.builder()
                .title("Архитектор")
                .build();

        when(positionRepository.existsByTitle("Архитектор")).thenReturn(false);
        when(positionRepository.save(any(Position.class))).thenAnswer(i -> {
            Position p = i.getArgument(0);
            p.setId(2L);
            return p;
        });

        PositionDto.Response created = positionService.create(request);

        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getTitle()).isEqualTo("Архитектор");
    }

    @Test
    @DisplayName("Create position with duplicate title throws BusinessConflictException")
    void testCreateDuplicateTitleThrowsException() {
        PositionDto.Request request = PositionDto.Request.builder()
                .title("Ведущий разработчик")
                .build();

        when(positionRepository.existsByTitle("Ведущий разработчик")).thenReturn(true);

        assertThatThrownBy(() -> positionService.create(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже существует");

        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update position with unique new title succeeds")
    void testUpdateSuccess() {
        PositionDto.Request request = PositionDto.Request.builder()
                .title("Главный разработчик")
                .build();

        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.existsByTitle("Главный разработчик")).thenReturn(false);
        when(positionRepository.save(any(Position.class))).thenAnswer(i -> i.getArgument(0));

        PositionDto.Response updated = positionService.update(1L, request);

        assertThat(updated.getTitle()).isEqualTo("Главный разработчик");
        assertThat(position.getTitle()).isEqualTo("Главный разработчик");
    }

    @Test
    @DisplayName("Update position with existing conflict title throws BusinessConflictException")
    void testUpdateConflictingTitleThrowsException() {
        PositionDto.Request request = PositionDto.Request.builder()
                .title("Тестировщик")
                .build();

        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(positionRepository.existsByTitle("Тестировщик")).thenReturn(true);

        assertThatThrownBy(() -> positionService.update(1L, request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("уже занята другим определением");

        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete position succeeds")
    void testDeleteSuccess() {
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        positionService.delete(1L);

        verify(positionRepository).delete(position);
    }
}

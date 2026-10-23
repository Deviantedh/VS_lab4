package portal.employee.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.PositionDto;
import portal.employee.service.PositionService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionControllerTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private PositionController positionController;

    @Test
    @DisplayName("getAll returns positions list")
    void testGetAll() {
        PositionDto.Response resp = PositionDto.Response.builder().id(1L).title("Developer").build();
        when(positionService.getAll()).thenReturn(List.of(resp));

        ResponseEntity<List<PositionDto.Response>> response = positionController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns position by id")
    void testGetById() {
        PositionDto.Response resp = PositionDto.Response.builder().id(1L).title("Developer").build();
        when(positionService.getById(1L)).thenReturn(resp);

        ResponseEntity<PositionDto.Response> response = positionController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created position with 201")
    void testCreate() {
        PositionDto.Request req = PositionDto.Request.builder().title("Manager").build();
        PositionDto.Response resp = PositionDto.Response.builder().id(2L).title("Manager").build();
        when(positionService.create(req)).thenReturn(resp);

        ResponseEntity<PositionDto.Response> response = positionController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("update returns updated position")
    void testUpdate() {
        PositionDto.Request req = PositionDto.Request.builder().title("Senior Manager").build();
        PositionDto.Response resp = PositionDto.Response.builder().id(1L).title("Senior Manager").build();
        when(positionService.update(1L, req)).thenReturn(resp);

        ResponseEntity<PositionDto.Response> response = positionController.update(1L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(positionService).delete(1L);

        ResponseEntity<Void> response = positionController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(positionService).delete(1L);
    }
}

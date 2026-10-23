package portal.schedule.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.ScheduleDto;
import portal.schedule.service.ScheduleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ScheduleController scheduleController;

    @Test
    @DisplayName("getByBranch returns list of schedules")
    void testGetByBranch() {
        ScheduleDto.Response resp = ScheduleDto.Response.builder().id(1L).branchId(10L).build();
        when(scheduleService.getByBranch(10L)).thenReturn(List.of(resp));

        ResponseEntity<List<ScheduleDto.Response>> response = scheduleController.getByBranch(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns schedule by id")
    void testGetById() {
        ScheduleDto.Response resp = ScheduleDto.Response.builder().id(1L).branchId(10L).build();
        when(scheduleService.getById(1L)).thenReturn(resp);

        ResponseEntity<ScheduleDto.Response> response = scheduleController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created schedule with 201")
    void testCreate() {
        ScheduleDto.Request req = ScheduleDto.Request.builder().branchId(10L).build();
        ScheduleDto.Response resp = ScheduleDto.Response.builder().id(2L).branchId(10L).build();
        when(scheduleService.create(req)).thenReturn(resp);

        ResponseEntity<ScheduleDto.Response> response = scheduleController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(scheduleService).delete(1L);

        ResponseEntity<Void> response = scheduleController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(scheduleService).delete(1L);
    }
}

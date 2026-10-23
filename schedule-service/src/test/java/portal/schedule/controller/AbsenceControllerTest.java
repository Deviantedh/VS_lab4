package portal.schedule.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.AbsenceType;
import portal.dto.EmployeeAbsenceDto;
import portal.schedule.service.AbsenceService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceControllerTest {

    @Mock
    private AbsenceService absenceService;

    @InjectMocks
    private AbsenceController absenceController;

    @Test
    @DisplayName("getAll returns list of absences")
    void testGetAll() {
        EmployeeAbsenceDto.Response resp = EmployeeAbsenceDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 6, 1))
                .dateTo(LocalDate.of(2026, 6, 14))
                .build();
        when(absenceService.getAll()).thenReturn(List.of(resp));

        ResponseEntity<List<EmployeeAbsenceDto.Response>> response = absenceController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getByEmployee returns list of absences for employee")
    void testGetByEmployee() {
        EmployeeAbsenceDto.Response resp = EmployeeAbsenceDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .type(AbsenceType.SICK_LEAVE)
                .dateFrom(LocalDate.of(2026, 6, 1))
                .dateTo(LocalDate.of(2026, 6, 5))
                .build();
        when(absenceService.getByEmployee(10L)).thenReturn(List.of(resp));

        ResponseEntity<List<EmployeeAbsenceDto.Response>> response = absenceController.getByEmployee(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("create returns created absence with 201")
    void testCreate() {
        EmployeeAbsenceDto.Request req = EmployeeAbsenceDto.Request.builder()
                .employeeId(10L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 14))
                .build();
        EmployeeAbsenceDto.Response resp = EmployeeAbsenceDto.Response.builder()
                .id(2L)
                .employeeId(10L)
                .type(AbsenceType.VACATION)
                .dateFrom(LocalDate.of(2026, 7, 1))
                .dateTo(LocalDate.of(2026, 7, 14))
                .build();
        when(absenceService.create(req)).thenReturn(resp);

        ResponseEntity<EmployeeAbsenceDto.Response> response = absenceController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }
}

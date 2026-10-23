package portal.schedule.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.RequestDto;
import portal.dto.RequestStatus;
import portal.dto.RequestType;
import portal.schedule.service.RequestService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    @Test
    @DisplayName("getAll returns paged requests")
    void testGetAll() {
        RequestDto.Response resp = RequestDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .type(RequestType.VACATION)
                .status(RequestStatus.PENDING)
                .build();
        Page<RequestDto.Response> page = new PageImpl<>(List.of(resp));
        when(requestService.getAllPaged(eq(RequestStatus.PENDING), any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<RequestDto.Response>> response = requestController.getAll(
                RequestStatus.PENDING, 0, 20, "createdAt", Sort.Direction.DESC
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns request by id")
    void testGetById() {
        RequestDto.Response resp = RequestDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .type(RequestType.VACATION)
                .status(RequestStatus.PENDING)
                .build();
        when(requestService.getById(1L)).thenReturn(resp);

        ResponseEntity<RequestDto.Response> response = requestController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("getByEmployee returns list of requests for employee")
    void testGetByEmployee() {
        RequestDto.Response resp = RequestDto.Response.builder()
                .id(1L)
                .employeeId(10L)
                .type(RequestType.DAY_OFF)
                .status(RequestStatus.APPROVED)
                .build();
        when(requestService.getByEmployee(10L)).thenReturn(List.of(resp));

        ResponseEntity<List<RequestDto.Response>> response = requestController.getByEmployee(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("create returns created request with 201")
    void testCreate() {
        RequestDto.Create req = RequestDto.Create.builder()
                .employeeId(10L)
                .type(RequestType.VACATION)
                .requestData("{\"dateFrom\":\"2026-10-01\",\"dateTo\":\"2026-10-14\"}")
                .build();
        RequestDto.Response resp = RequestDto.Response.builder()
                .id(2L)
                .employeeId(10L)
                .type(RequestType.VACATION)
                .status(RequestStatus.PENDING)
                .build();
        when(requestService.create(req)).thenReturn(resp);

        ResponseEntity<RequestDto.Response> response = requestController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("process returns processed request with 200")
    void testProcess() {
        RequestDto.Process proc = RequestDto.Process.builder()
                .status(RequestStatus.APPROVED)
                .processedById(1L)
                .resolutionComment("Approved by HR")
                .build();
        RequestDto.Response resp = RequestDto.Response.builder()
                .id(2L)
                .employeeId(10L)
                .type(RequestType.VACATION)
                .status(RequestStatus.APPROVED)
                .resolutionComment("Approved by HR")
                .build();
        when(requestService.processRequest(2L, proc)).thenReturn(resp);

        ResponseEntity<RequestDto.Response> response = requestController.process(2L, proc);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }
}

package portal.employee.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import portal.dto.CompanyDto;
import portal.employee.service.CompanyService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private CompanyController companyController;

    @Test
    @DisplayName("getAll returns company list")
    void testGetAll() {
        CompanyDto.Response resp = CompanyDto.Response.builder().id(1L).name("Test Co").build();
        when(companyService.getAll()).thenReturn(List.of(resp));

        ResponseEntity<List<CompanyDto.Response>> response = companyController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns company by id")
    void testGetById() {
        CompanyDto.Response resp = CompanyDto.Response.builder().id(1L).name("Test Co").build();
        when(companyService.getById(1L)).thenReturn(resp);

        ResponseEntity<CompanyDto.Response> response = companyController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created company with status 201")
    void testCreate() {
        CompanyDto.Request req = CompanyDto.Request.builder().name("New Co").build();
        CompanyDto.Response resp = CompanyDto.Response.builder().id(2L).name("New Co").build();
        when(companyService.create(req)).thenReturn(resp);

        ResponseEntity<CompanyDto.Response> response = companyController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("update returns updated company")
    void testUpdate() {
        CompanyDto.Request req = CompanyDto.Request.builder().name("Updated Co").build();
        CompanyDto.Response resp = CompanyDto.Response.builder().id(1L).name("Updated Co").build();
        when(companyService.update(1L, req)).thenReturn(resp);

        ResponseEntity<CompanyDto.Response> response = companyController.update(1L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(companyService).delete(1L);

        ResponseEntity<Void> response = companyController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(companyService).delete(1L);
    }
}

package portal.employee.controller;

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
import portal.dto.BranchDto;
import portal.employee.service.BranchService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    @Mock
    private BranchService branchService;

    @InjectMocks
    private BranchController branchController;

    @Test
    @DisplayName("getAll returns paged branches")
    void testGetAll() {
        BranchDto.Response resp = BranchDto.Response.builder().id(1L).name("Branch 1").build();
        Page<BranchDto.Response> page = new PageImpl<>(List.of(resp));
        when(branchService.getAllPaged(any(PageRequest.class))).thenReturn(page);

        ResponseEntity<Page<BranchDto.Response>> response = branchController.getAll(0, 20, "id", Sort.Direction.ASC);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(resp);
    }

    @Test
    @DisplayName("getByCompany returns list of branches")
    void testGetByCompany() {
        BranchDto.Response resp = BranchDto.Response.builder().id(1L).name("Branch 1").companyId(10L).build();
        when(branchService.getAllByCompany(10L)).thenReturn(List.of(resp));

        ResponseEntity<List<BranchDto.Response>> response = branchController.getByCompany(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(resp);
    }

    @Test
    @DisplayName("getById returns branch by id")
    void testGetById() {
        BranchDto.Response resp = BranchDto.Response.builder().id(1L).name("Branch 1").build();
        when(branchService.getById(1L)).thenReturn(resp);

        ResponseEntity<BranchDto.Response> response = branchController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("create returns created branch with status 201")
    void testCreate() {
        BranchDto.Request req = BranchDto.Request.builder().name("New Branch").build();
        BranchDto.Response resp = BranchDto.Response.builder().id(2L).name("New Branch").build();
        when(branchService.create(req)).thenReturn(resp);

        ResponseEntity<BranchDto.Response> response = branchController.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("update returns updated branch")
    void testUpdate() {
        BranchDto.Request req = BranchDto.Request.builder().name("Updated Branch").build();
        BranchDto.Response resp = BranchDto.Response.builder().id(1L).name("Updated Branch").build();
        when(branchService.update(1L, req)).thenReturn(resp);

        ResponseEntity<BranchDto.Response> response = branchController.update(1L, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resp);
    }

    @Test
    @DisplayName("delete returns 204 No Content")
    void testDelete() {
        doNothing().when(branchService).delete(1L);

        ResponseEntity<Void> response = branchController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(branchService).delete(1L);
    }
}

package portal.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portal.dto.CompanyDto;
import portal.employee.exception.ResourceNotFoundException;
import portal.employee.repository.CompanyRepository;
import portal.entity.Company;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    private CompanyService companyService;
    private Company company;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository);
        company = Company.builder()
                .id(1L)
                .name("ООО ТехноПарк")
                .build();
    }

    @Test
    @DisplayName("Get all companies returns DTO list")
    void testGetAll() {
        when(companyRepository.findAll()).thenReturn(List.of(company));

        List<CompanyDto.Response> result = companyService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("ООО ТехноПарк");
    }

    @Test
    @DisplayName("Get company by ID returns response")
    void testGetById() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyDto.Response result = companyService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("ООО ТехноПарк");
    }

    @Test
    @DisplayName("Get non-existent company by ID throws ResourceNotFoundException")
    void testGetByIdNotFoundThrowsException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    @DisplayName("Create company persists and returns response")
    void testCreateSuccess() {
        CompanyDto.Request request = CompanyDto.Request.builder()
                .name("Инновации 2026")
                .build();

        when(companyRepository.save(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(2L);
            return c;
        });

        CompanyDto.Response created = companyService.create(request);

        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getName()).isEqualTo("Инновации 2026");
    }

    @Test
    @DisplayName("Update company updates name and returns response")
    void testUpdateSuccess() {
        CompanyDto.Request request = CompanyDto.Request.builder()
                .name("ООО Новые Технологии")
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        CompanyDto.Response updated = companyService.update(1L, request);

        assertThat(updated.getName()).isEqualTo("ООО Новые Технологии");
        assertThat(company.getName()).isEqualTo("ООО Новые Технологии");
    }

    @Test
    @DisplayName("Delete company deletes entity")
    void testDeleteSuccess() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        companyService.delete(1L);

        verify(companyRepository).delete(company);
    }
}

package portal.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import portal.dto.BranchDto;
import portal.entity.Branch;
import portal.entity.Company;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.BranchRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private BranchService branchService;

    private Company company;
    private Branch branch;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("ITMO Corp")
                .build();

        branch = Branch.builder()
                .id(10L)
                .company(company)
                .name("Филиал на Кронверкском")
                .address("Кронверкский пр., 49, лит. А")
                .phone("+78121112233")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getAllByCompany should return all branches for given company")
    void testGetAllByCompany() {
        when(branchRepository.findAllByCompanyId(1L)).thenReturn(List.of(branch));

        List<BranchDto.Response> result = branchService.getAllByCompany(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Филиал на Кронверкском");
        assertThat(result.get(0).getCompanyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAllPaged should return paged responses")
    void testGetAllPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        when(branchRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(branch)));

        Page<BranchDto.Response> page = branchService.getAllPaged(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Филиал на Кронверкском");
    }

    @Test
    @DisplayName("getById should return branch response when found")
    void testGetByIdFound() {
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));

        BranchDto.Response response = branchService.getById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Филиал на Кронверкском");
    }

    @Test
    @DisplayName("getById should throw ResourceNotFoundException when not found")
    void testGetByIdNotFound() {
        when(branchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Филиал с ID 999 не найден");
    }

    @Test
    @DisplayName("create should save new branch with company")
    void testCreate() {
        BranchDto.Request request = BranchDto.Request.builder()
                .companyId(1L)
                .name("Новый филиал")
                .address("ул. Ломоносова, 9")
                .phone("+78129998877")
                .isActive(true)
                .build();

        when(companyService.findCompanyById(1L)).thenReturn(company);
        when(branchRepository.save(any(Branch.class))).thenAnswer(i -> {
            Branch b = i.getArgument(0);
            b.setId(20L);
            return b;
        });

        BranchDto.Response created = branchService.create(request);

        assertThat(created.getId()).isEqualTo(20L);
        assertThat(created.getName()).isEqualTo("Новый филиал");
        assertThat(created.getAddress()).isEqualTo("ул. Ломоносова, 9");
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    @DisplayName("update should update branch details and company if changed")
    void testUpdate() {
        Company newCompany = Company.builder().id(2L).name("Second Corp").build();
        BranchDto.Request updateReq = BranchDto.Request.builder()
                .companyId(2L)
                .name("Обновленный филиал")
                .address("ул. Грибоедова, 30")
                .phone("+78120000000")
                .isActive(false)
                .build();

        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(companyService.findCompanyById(2L)).thenReturn(newCompany);
        when(branchRepository.save(any(Branch.class))).thenAnswer(i -> i.getArgument(0));

        BranchDto.Response updated = branchService.update(10L, updateReq);

        assertThat(updated.getName()).isEqualTo("Обновленный филиал");
        assertThat(updated.getCompanyId()).isEqualTo(2L);
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("delete should remove branch")
    void testDelete() {
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));

        branchService.delete(10L);

        verify(branchRepository).delete(branch);
    }
}

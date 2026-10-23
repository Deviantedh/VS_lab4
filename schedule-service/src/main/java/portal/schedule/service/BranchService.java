package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.BranchDto;
import portal.entity.Branch;
import portal.entity.Company;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.BranchRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public List<BranchDto.Response> getAllByCompany(Long companyId) {
        return branchRepository.findAllByCompanyId(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BranchDto.Response> getAllPaged(Pageable pageable) {
        return branchRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BranchDto.Response getById(Long id) {
        return toResponse(findBranchById(id));
    }

    @Transactional
    public BranchDto.Response create(BranchDto.Request request) {
        Company company = companyService.findCompanyById(request.getCompanyId());

        Branch branch = Branch.builder()
                .company(company)
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .phone(request.getPhone())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchDto.Response update(Long id, BranchDto.Request request) {
        Branch branch = findBranchById(id);

        if (!branch.getCompany().getId().equals(request.getCompanyId())) {
            Company company = companyService.findCompanyById(request.getCompanyId());
            branch.setCompany(company);
        }

        branch.setName(request.getName().trim());
        branch.setAddress(request.getAddress().trim());
        branch.setPhone(request.getPhone());
        if (request.getIsActive() != null) {
            branch.setIsActive(request.getIsActive());
        }

        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = findBranchById(id);
        branchRepository.delete(branch);
    }

    public Branch findBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Филиал с ID " + id + " не найден: возможно, точка закрылась на переучёт или переехала на соседнюю улицу"));
    }

    public BranchDto.Response toResponse(Branch branch) {
        return BranchDto.Response.builder()
                .id(branch.getId())
                .companyId(branch.getCompany().getId())
                .companyName(branch.getCompany().getName())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .isActive(branch.getIsActive())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}

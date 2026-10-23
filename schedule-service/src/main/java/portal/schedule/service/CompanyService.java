package portal.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portal.dto.CompanyDto;
import portal.entity.Company;
import portal.schedule.exception.ResourceNotFoundException;
import portal.schedule.repository.CompanyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<CompanyDto.Response> getAll() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDto.Response getById(Long id) {
        return toResponse(findCompanyById(id));
    }

    @Transactional
    public CompanyDto.Response create(CompanyDto.Request request) {
        Company company = Company.builder()
                .name(request.getName().trim())
                .build();
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyDto.Response update(Long id, CompanyDto.Request request) {
        Company company = findCompanyById(id);
        company.setName(request.getName().trim());
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public void delete(Long id) {
        Company company = findCompanyById(id);
        companyRepository.delete(company);
    }

    public Company findCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Компания с ID " + id + " не найдена: кажется, организация пока существует только в планах"));
    }

    private CompanyDto.Response toResponse(Company company) {
        return CompanyDto.Response.builder()
                .id(company.getId())
                .name(company.getName())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}

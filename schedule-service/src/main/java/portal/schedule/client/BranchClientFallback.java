package portal.schedule.client;

import org.springframework.stereotype.Component;
import portal.dto.BranchDto;

@Component
public class BranchClientFallback implements BranchClient {

    @Override
    public BranchDto.Response getBranchById(Long id) {
        return BranchDto.Response.builder()
                .id(id)
                .name("Резервный филиал (Employee Service временно недоступен)")
                .address("Адрес недоступен")
                .phone("+0000000000")
                .isActive(true)
                .companyId(1L)
                .build();
    }
}

package portal.schedule.client;

import org.springframework.stereotype.Component;
import portal.dto.EmployeeDto;
import portal.dto.EmployeeStatus;

import java.time.LocalDate;

@Component
public class EmployeeClientFallback implements EmployeeClient {

    @Override
    public EmployeeDto.Response getEmployeeById(Long id) {
        // заглушка при срабатывании Circuit Breaker
        return EmployeeDto.Response.builder()
                .id(id)
                .name("Резервный профиль (Employee Service временно недоступен)")
                .phone("+0000000000")
                .hireDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .build();
    }
}

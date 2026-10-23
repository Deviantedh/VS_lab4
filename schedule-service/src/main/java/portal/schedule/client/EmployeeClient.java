package portal.schedule.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import portal.dto.EmployeeDto;

@FeignClient(name = "employee-service", fallback = EmployeeClientFallback.class)
public interface EmployeeClient {

    @GetMapping("/api/employees/{id}")
    EmployeeDto.Response getEmployeeById(@PathVariable("id") Long id);
}

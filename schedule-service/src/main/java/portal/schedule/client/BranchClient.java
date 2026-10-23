package portal.schedule.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import portal.dto.BranchDto;

@FeignClient(name = "employee-service", contextId = "branchClient", fallback = BranchClientFallback.class)
public interface BranchClient {

    @GetMapping("/api/branches/{id}")
    BranchDto.Response getBranchById(@PathVariable("id") Long id);
}

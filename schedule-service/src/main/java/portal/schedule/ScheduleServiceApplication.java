package portal.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({"portal.schedule", "portal.entity"})
@EnableJpaRepositories(basePackages = "portal.schedule.repository")
@EnableDiscoveryClient
@EnableFeignClients
public class ScheduleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleServiceApplication.class, args);
    }
}

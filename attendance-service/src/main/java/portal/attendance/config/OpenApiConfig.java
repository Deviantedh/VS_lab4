package portal.attendance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI attendanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Attendance Reactive Service API (R2DBC)")
                        .version("2.0.0")
                        .description("Реактивный сервис учета явки сотрудников на базе Spring WebFlux и R2DBC"));
    }
}

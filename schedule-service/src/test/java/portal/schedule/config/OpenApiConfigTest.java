package portal.schedule.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("scheduleOpenAPI configures title and version")
    void testScheduleOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.scheduleOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).contains("Schedule & Shift Management");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
    }
}

package portal.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class GatewayServiceApplicationTest {

    @Test
    @DisplayName("Application class has discovery client annotation and can be instantiated")
    void testApplicationClass() {
        GatewayServiceApplication app = new GatewayServiceApplication();
        assertThat(app).isNotNull();
        assertThat(GatewayServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class)).isTrue();
    }

    @Test
    @DisplayName("main method delegates to SpringApplication.run")
    void testMainMethod() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(GatewayServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            GatewayServiceApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(GatewayServiceApplication.class, new String[]{}));
        }
    }
}

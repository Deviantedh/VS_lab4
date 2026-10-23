package portal.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class DiscoveryServiceApplicationTest {

    @Test
    @DisplayName("Application class has Eureka server annotation and can be instantiated")
    void testApplicationClass() {
        DiscoveryServiceApplication app = new DiscoveryServiceApplication();
        assertThat(app).isNotNull();
        assertThat(DiscoveryServiceApplication.class.isAnnotationPresent(EnableEurekaServer.class)).isTrue();
    }

    @Test
    @DisplayName("main method delegates to SpringApplication.run")
    void testMainMethod() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(DiscoveryServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            DiscoveryServiceApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(DiscoveryServiceApplication.class, new String[]{}));
        }
    }
}

package portal.configserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class ConfigServiceApplicationTest {

    @Test
    @DisplayName("Application class has Config Server annotations and can be instantiated")
    void testApplicationClass() {
        ConfigServiceApplication app = new ConfigServiceApplication();
        assertThat(app).isNotNull();
        assertThat(ConfigServiceApplication.class.isAnnotationPresent(EnableConfigServer.class)).isTrue();
        assertThat(ConfigServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class)).isTrue();
    }

    @Test
    @DisplayName("main method delegates to SpringApplication.run")
    void testMainMethod() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ConfigServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            ConfigServiceApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(ConfigServiceApplication.class, new String[]{}));
        }
    }
}

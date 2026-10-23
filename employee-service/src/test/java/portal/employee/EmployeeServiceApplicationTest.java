package portal.employee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class EmployeeServiceApplicationTest {

    @Test
    @DisplayName("Application class has discovery client annotation and can be instantiated")
    void testApplicationClass() {
        EmployeeServiceApplication app = new EmployeeServiceApplication();
        assertThat(app).isNotNull();
        assertThat(EmployeeServiceApplication.class.isAnnotationPresent(EnableDiscoveryClient.class)).isTrue();
    }

    @Test
    @DisplayName("main method delegates to SpringApplication.run")
    void testMainMethod() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(EmployeeServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            EmployeeServiceApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(EmployeeServiceApplication.class, new String[]{}));
        }
    }
}

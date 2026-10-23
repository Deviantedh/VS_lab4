package portal.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleServiceApplicationTest {

    @Test
    @DisplayName("Application instance can be created")
    void testApplicationInstance() {
        ScheduleServiceApplication application = new ScheduleServiceApplication();
        assertThat(application).isNotNull();
    }
}

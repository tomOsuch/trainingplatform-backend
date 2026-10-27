package pl.tomaszosuch.trainingplatform_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import pl.tomaszosuch.trainingplatform_backend.support.PostgresContainerSupport;

@SpringBootTest
class TrainingplatformBackendApplicationTests extends PostgresContainerSupport {

    @Test
    void contextLoads() {
    }

}
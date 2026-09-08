package com.opsflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OpsFlowApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context initializes cleanly with test profile
    }
}

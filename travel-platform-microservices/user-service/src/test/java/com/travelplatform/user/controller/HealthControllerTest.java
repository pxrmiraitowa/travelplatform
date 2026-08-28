package com.travelplatform.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HealthControllerTest {
    @Test
    void exposesHealthAndVersionContracts() {
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "serviceName", "user-service");
        ReflectionTestUtils.setField(controller, "version", "test-version");
        assertEquals("UP", controller.health().getData().status());
        assertEquals("test-version", controller.version().getData().version());
        assertNotNull(controller.version().getData().startedAt());
    }
}

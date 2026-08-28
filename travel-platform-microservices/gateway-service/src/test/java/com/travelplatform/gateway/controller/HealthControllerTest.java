package com.travelplatform.gateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HealthControllerTest {
    @SuppressWarnings("unchecked")
    @Test
    void exposesHealthAndVersionContracts() {
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "serviceName", "gateway-service");
        ReflectionTestUtils.setField(controller, "version", "test-version");
        Map<String, Object> healthData = (Map<String, Object>) controller.health().get("data");
        Map<String, Object> versionData = (Map<String, Object>) controller.version().get("data");
        assertEquals("UP", healthData.get("status"));
        assertEquals("test-version", versionData.get("version"));
    }
}

package com.travelplatform;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TravelPlatformServerApplicationTests {

    @Test
    void applicationClassShouldBeAvailable() {
        assertThat(TravelPlatformServerApplication.class).isNotNull();
    }
}

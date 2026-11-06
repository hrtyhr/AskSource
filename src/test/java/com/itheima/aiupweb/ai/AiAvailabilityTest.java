package com.itheima.aiupweb.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAvailabilityTest {

    @Test
    void reportsUnavailableWithoutConfiguredKey() {
        AiProperties properties = new AiProperties();
        AiAvailability availability = new AiAvailability(properties, AiAvailability.UNCONFIGURED_KEY);

        assertThat(availability.isAvailable()).isFalse();
        assertThat(availability.statusMessage()).contains("DASHSCOPE_API_KEY");
    }

    @Test
    void reportsUnavailableWhenDisabledEvenWithKey() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);
        AiAvailability availability = new AiAvailability(properties, "sk-test");

        assertThat(availability.isAvailable()).isFalse();
        assertThat(availability.statusMessage()).contains("关闭");
    }

    @Test
    void reportsAvailableWithKeyAndEnabled() {
        AiProperties properties = new AiProperties();
        AiAvailability availability = new AiAvailability(properties, "sk-test");

        assertThat(availability.isAvailable()).isTrue();
    }
}

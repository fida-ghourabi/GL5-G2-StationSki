package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in metrics work with isolated H2; sensitive Actuator endpoints stay hidden. */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics
@AutoConfigureMockMvc
@ActiveProfiles({"crud-test", "monitoring"})
class MonitoringEndpointTest {
    @Autowired private MockMvc mvc;

    @Test
    void exposesPrometheusMetricsWithApplicationTag() throws Exception {
        mvc.perform(get("/stationSki/actuator/prometheus").contextPath("/stationSki"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("application=\"stationski\"")));
    }

    @Test
    void doesNotExposeEnvironmentEndpoint() throws Exception {
        mvc.perform(get("/stationSki/actuator/env").contextPath("/stationSki"))
                .andExpect(status().isNotFound());
    }
}

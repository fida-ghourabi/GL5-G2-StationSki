package com.example.gestionstationskii.integration;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Skier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RegistrationApiIntegrationTest extends RegistrationIntegrationSupport {
    @Autowired private MockMvc mvc;

    @Test
    void registrationLifecycleWorksThroughHttpAndPersistsItsState() throws Exception {
        Course course = createCourse();
        Instructor instructor = createInstructor(course);
        Skier skier = createAdultSkier();
        PlanningSlot slot = createSlot(course, instructor, 1);

        String response = mvc.perform(post("/registration/skier/{skierId}/slot/{slotId}",
                        skier.getNumSkier(), slot.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(false))
                .andReturn().getResponse().getContentAsString();
        long registrationId = Long.parseLong(
                com.jayway.jsonpath.JsonPath.read(response, "$.numRegistration").toString());

        assertEquals(com.example.gestionstationskii.entities.SlotStatus.FULL,
                slots.findById(slot.getId()).orElseThrow(AssertionError::new).getStatus());
        mvc.perform(get("/registration/skier/{skierId}", skier.getNumSkier()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].numRegistration").value(registrationId));

        mvc.perform(delete("/registration/{id}", registrationId)).andExpect(status().isOk());

        Registration cancelled = registrations.findById(registrationId).orElseThrow(AssertionError::new);
        assertTrue(cancelled.isCancelled());
        assertEquals(com.example.gestionstationskii.entities.SlotStatus.OPEN,
                slots.findById(slot.getId()).orElseThrow(AssertionError::new).getStatus());
    }
}

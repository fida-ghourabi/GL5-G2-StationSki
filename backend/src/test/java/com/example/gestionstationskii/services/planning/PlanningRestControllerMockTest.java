package com.example.gestionstationskii.services.planning;

import com.example.gestionstationskii.controllers.ApiExceptionHandler;
import com.example.gestionstationskii.controllers.PlanningRestController;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.InstructorAvailability;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.services.IPlanningServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlanningRestControllerMockTest {
    private IPlanningServices service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(IPlanningServices.class);
        mvc = MockMvcBuilders.standaloneSetup(new PlanningRestController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void availabilityRouteBindsInstructorAndRequestBody() throws Exception {
        when(service.addAvailability(eq(4L), any(Requests.Availability.class)))
                .thenAnswer(invocation -> new InstructorAvailability());

        mvc.perform(post("/planning/availability/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00:00\",\"endTime\":\"12:00:00\",\"validFrom\":\"2027-01-04\",\"validUntil\":\"2027-02-01\"}"))
                .andExpect(status().isOk());

        verify(service).addAvailability(eq(4L), argThat(input -> input.getDayOfWeek().name().equals("MONDAY")
                && input.getStartTime().toString().equals("09:00")
                && input.getValidFrom().toString().equals("2027-01-04")));
    }

    @Test
    void slotSearchRoutesPassDateAndFiltersToService() throws Exception {
        when(service.search(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(), anyBoolean()))
                .thenReturn(Collections.<PlanningSlot>emptyList());

        mvc.perform(get("/planning/slots")
                        .param("from", "2027-01-04T09:00:00")
                        .param("to", "2027-01-04T12:00:00")
                        .param("courseId", "10")
                        .param("instructorId", "4"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).search(LocalDateTime.parse("2027-01-04T09:00:00"),
                LocalDateTime.parse("2027-01-04T12:00:00"), 10L, 4L, false);

        mvc.perform(get("/planning/slots/available")
                        .param("from", "2027-01-04T09:00:00")
                        .param("to", "2027-01-04T12:00:00")
                        .param("courseId", "10"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).search(LocalDateTime.parse("2027-01-04T09:00:00"),
                LocalDateTime.parse("2027-01-04T12:00:00"), 10L, null, true);
    }

    @Test
    void slotMutationRoutesDelegateToPlanningService() throws Exception {
        when(service.create(any(Requests.Slot.class))).thenReturn(new PlanningSlot());
        when(service.get(5L)).thenReturn(new PlanningSlot());
        when(service.update(eq(5L), any(Requests.Slot.class))).thenReturn(new PlanningSlot());

        mvc.perform(post("/planning/slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"instructorId\":4,\"startTime\":\"2027-01-04T10:00:00\",\"endTime\":\"2027-01-04T11:00:00\",\"maxCapacity\":6}"))
                .andExpect(status().isOk());
        mvc.perform(get("/planning/slots/5")).andExpect(status().isOk());
        mvc.perform(put("/planning/slots/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":10,\"instructorId\":4,\"startTime\":\"2027-01-04T10:00:00\",\"endTime\":\"2027-01-04T11:00:00\",\"maxCapacity\":6}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/planning/slots/5")).andExpect(status().isOk());

        verify(service).create(any(Requests.Slot.class));
        verify(service).get(5L);
        verify(service).update(eq(5L), any(Requests.Slot.class));
        verify(service).cancel(5L);
    }
}

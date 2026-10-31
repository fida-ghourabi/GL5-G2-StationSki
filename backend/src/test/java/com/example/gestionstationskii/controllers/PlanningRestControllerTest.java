package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.InstructorAvailability;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.SlotStatus;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPlanningServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanningRestControllerTest {

    private static final String SLOT_JSON = "{\"courseId\":3,\"instructorId\":2,"
            + "\"startTime\":\"2030-02-04T10:00:00\",\"endTime\":\"2030-02-04T11:00:00\",\"maxCapacity\":4}";
    private static final String AVAILABILITY_JSON = "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00:00\","
            + "\"endTime\":\"12:00:00\",\"validFrom\":\"2030-01-01\",\"validUntil\":\"2030-03-31\"}";

    @Mock
    private IPlanningServices service;

    @InjectMocks
    private PlanningRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static PlanningSlot slot(Long id) {
        PlanningSlot s = new PlanningSlot();
        s.setId(id);
        s.setMaxCapacity(4);
        s.setStatus(SlotStatus.OPEN);
        return s;
    }

    @Test
    void addAvailabilityConvertsPayloadAndDelegates() throws Exception {
        InstructorAvailability saved = new InstructorAvailability();
        saved.setId(11L);
        saved.setDayOfWeek(DayOfWeek.MONDAY);
        when(service.addAvailability(eq(2L), any(Requests.Availability.class))).thenReturn(saved);

        mockMvc.perform(post("/planning/availability/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AVAILABILITY_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"));

        ArgumentCaptor<Requests.Availability> captor = ArgumentCaptor.forClass(Requests.Availability.class);
        verify(service).addAvailability(eq(2L), captor.capture());
        assertEquals(DayOfWeek.MONDAY, captor.getValue().getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), captor.getValue().getStartTime());
        assertEquals(LocalTime.of(12, 0), captor.getValue().getEndTime());
        assertEquals(LocalDate.of(2030, 1, 1), captor.getValue().getValidFrom());
        assertEquals(LocalDate.of(2030, 3, 31), captor.getValue().getValidUntil());
    }

    @Test
    void addAvailabilityRejectsInvalidDayOfWeek() throws Exception {
        mockMvc.perform(post("/planning/availability/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"FUNDAY\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void availabilityReturnsInstructorAvailabilities() throws Exception {
        InstructorAvailability a = new InstructorAvailability();
        a.setId(1L);
        when(service.availability(2L)).thenReturn(Collections.singletonList(a));

        mockMvc.perform(get("/planning/instructors/2/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void availabilityMapsMissingInstructorToNotFound() throws Exception {
        when(service.availability(9L)).thenThrow(BusinessException.missing("Moniteur"));

        mockMvc.perform(get("/planning/instructors/9/availability"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Moniteur introuvable"));
    }

    @Test
    void createConvertsPayloadAndDelegates() throws Exception {
        when(service.create(any(Requests.Slot.class))).thenReturn(slot(5L));

        mockMvc.perform(post("/planning/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SLOT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("OPEN"));

        ArgumentCaptor<Requests.Slot> captor = ArgumentCaptor.forClass(Requests.Slot.class);
        verify(service).create(captor.capture());
        assertEquals(Long.valueOf(3L), captor.getValue().getCourseId());
        assertEquals(Long.valueOf(2L), captor.getValue().getInstructorId());
        assertEquals(LocalDateTime.of(2030, 2, 4, 10, 0), captor.getValue().getStartTime());
        assertEquals(LocalDateTime.of(2030, 2, 4, 11, 0), captor.getValue().getEndTime());
        assertEquals(Integer.valueOf(4), captor.getValue().getMaxCapacity());
    }

    @Test
    void createMapsOverlapToConflict() throws Exception {
        when(service.create(any(Requests.Slot.class)))
                .thenThrow(BusinessException.conflict("Le moniteur a déjà un cours sur ce créneau"));

        mockMvc.perform(post("/planning/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SLOT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createRejectsMalformedDate() throws Exception {
        mockMvc.perform(post("/planning/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":3,\"instructorId\":2,\"startTime\":\"demain\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getReturnsSlot() throws Exception {
        when(service.get(5L)).thenReturn(slot(5L));

        mockMvc.perform(get("/planning/slots/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.maxCapacity").value(4));
    }

    @Test
    void getMapsMissingSlotToNotFound() throws Exception {
        when(service.get(9L)).thenThrow(BusinessException.missing("Créneau"));

        mockMvc.perform(get("/planning/slots/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDelegatesWithIdentifierAndPayload() throws Exception {
        when(service.update(eq(5L), any(Requests.Slot.class))).thenReturn(slot(5L));

        mockMvc.perform(put("/planning/slots/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SLOT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(service).update(eq(5L), any(Requests.Slot.class));
    }

    @Test
    void cancelDelegatesToService() throws Exception {
        mockMvc.perform(delete("/planning/slots/5"))
                .andExpect(status().isOk());

        verify(service).cancel(5L);
    }

    @Test
    void cancelMapsStartedSlotToConflict() throws Exception {
        doThrow(BusinessException.conflict("Créneau commencé non annulable")).when(service).cancel(5L);

        mockMvc.perform(delete("/planning/slots/5"))
                .andExpect(status().isConflict());
    }

    @Test
    void searchPassesPeriodAndFiltersWithoutAvailabilityFlag() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 2, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 2, 5, 0, 0);
        when(service.search(from, to, 3L, 2L, false)).thenReturn(Arrays.asList(slot(1L), slot(2L)));

        mockMvc.perform(get("/planning/slots")
                        .param("from", "2030-02-04T00:00:00")
                        .param("to", "2030-02-05T00:00:00")
                        .param("courseId", "3")
                        .param("instructorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(service).search(from, to, 3L, 2L, false);
    }

    @Test
    void searchWithoutOptionalFiltersPassesNulls() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 2, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 2, 5, 0, 0);
        when(service.search(eq(from), eq(to), isNull(), isNull(), eq(false)))
                .thenReturn(Collections.<PlanningSlot>emptyList());

        mockMvc.perform(get("/planning/slots")
                        .param("from", "2030-02-04T00:00:00")
                        .param("to", "2030-02-05T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(service).search(eq(from), eq(to), isNull(), isNull(), eq(false));
    }

    @Test
    void searchRequiresPeriodAndValidDates() throws Exception {
        mockMvc.perform(get("/planning/slots"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/planning/slots")
                        .param("from", "pas-une-date")
                        .param("to", "2030-02-05T00:00:00"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void availableSearchSetsAvailabilityFlag() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 2, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 2, 5, 0, 0);
        when(service.search(eq(from), eq(to), eq(3L), isNull(), eq(true)))
                .thenReturn(Collections.singletonList(slot(1L)));

        mockMvc.perform(get("/planning/slots/available")
                        .param("from", "2030-02-04T00:00:00")
                        .param("to", "2030-02-05T00:00:00")
                        .param("courseId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(service).search(eq(from), eq(to), eq(3L), isNull(), eq(true));
    }
}

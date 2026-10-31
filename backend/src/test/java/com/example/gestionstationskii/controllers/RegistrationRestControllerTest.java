package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IRegistrationServices;
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

import java.time.LocalDateTime;
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
class RegistrationRestControllerTest {

    private static final String REGISTRATION_JSON = "{\"numWeek\":5,\"planningSlot\":{\"id\":7}}";

    @Mock
    private IRegistrationServices registrationServices;

    @InjectMocks
    private RegistrationRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static Registration registration(Long id) {
        Registration r = new Registration();
        r.setNumRegistration(id);
        r.setNumWeek(5);
        return r;
    }

    @Test
    void registerDelegatesSkierAndSlotIdentifiers() throws Exception {
        when(registrationServices.registerSlot(1L, 7L)).thenReturn(registration(10L));

        mockMvc.perform(post("/registration/skier/1/slot/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numRegistration").value(10))
                .andExpect(jsonPath("$.numWeek").value(5))
                .andExpect(jsonPath("$.cancelled").value(false));

        verify(registrationServices).registerSlot(1L, 7L);
    }

    @Test
    void registerMapsConflictWhenSlotIsFull() throws Exception {
        when(registrationServices.registerSlot(1L, 7L))
                .thenThrow(BusinessException.conflict("Capacité du créneau atteinte"));

        mockMvc.perform(post("/registration/skier/1/slot/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Capacité du créneau atteinte"))
                .andExpect(jsonPath("$.path").value("/registration/skier/1/slot/7"));
    }

    @Test
    void registerMapsMissingSkierToNotFound() throws Exception {
        when(registrationServices.registerSlot(99L, 7L)).thenThrow(BusinessException.missing("Skieur"));

        mockMvc.perform(post("/registration/skier/99/slot/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bySkierReturnsHistory() throws Exception {
        when(registrationServices.bySkier(1L)).thenReturn(Arrays.asList(registration(10L), registration(11L)));

        mockMvc.perform(get("/registration/skier/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].numRegistration").value(11));
    }

    @Test
    void cancelDelegatesToService() throws Exception {
        mockMvc.perform(delete("/registration/10"))
                .andExpect(status().isOk());

        verify(registrationServices).cancel(10L);
    }

    @Test
    void cancelMapsStartedLessonToConflict() throws Exception {
        doThrow(BusinessException.conflict("Inscription commencée non annulable")).when(registrationServices).cancel(10L);

        mockMvc.perform(delete("/registration/10"))
                .andExpect(status().isConflict());
    }

    @Test
    void searchPassesPeriodAndCourse() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 2, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 2, 11, 0, 0);
        when(registrationServices.search(from, to, 3L)).thenReturn(Collections.singletonList(registration(10L)));

        mockMvc.perform(get("/registration")
                        .param("from", "2030-02-04T00:00:00")
                        .param("to", "2030-02-11T00:00:00")
                        .param("courseId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(registrationServices).search(from, to, 3L);
    }

    @Test
    void searchWithoutCourseFilterPassesNull() throws Exception {
        LocalDateTime from = LocalDateTime.of(2030, 2, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 2, 11, 0, 0);
        when(registrationServices.search(eq(from), eq(to), isNull())).thenReturn(Collections.<Registration>emptyList());

        mockMvc.perform(get("/registration")
                        .param("from", "2030-02-04T00:00:00")
                        .param("to", "2030-02-11T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchRequiresPeriod() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationServices);
    }

    @Test
    void addAndAssignToSkierConvertsSlotReference() throws Exception {
        when(registrationServices.addRegistrationAndAssignToSkier(any(Registration.class), eq(1L)))
                .thenReturn(registration(10L));

        mockMvc.perform(put("/registration/addAndAssignToSkier/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numRegistration").value(10));

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationServices).addRegistrationAndAssignToSkier(captor.capture(), eq(1L));
        assertEquals(5, captor.getValue().getNumWeek());
        assertNotNull(captor.getValue().getPlanningSlot());
        assertEquals(Long.valueOf(7L), captor.getValue().getPlanningSlot().getId());
    }

    @Test
    void addAndAssignToSkierWithoutSlotPassesRegistrationWithoutSlot() throws Exception {
        when(registrationServices.addRegistrationAndAssignToSkier(any(Registration.class), eq(1L)))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "Créneau requis"));

        mockMvc.perform(put("/registration/addAndAssignToSkier/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numWeek\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Créneau requis"));

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationServices).addRegistrationAndAssignToSkier(captor.capture(), eq(1L));
        assertNull(captor.getValue().getPlanningSlot());
    }

    @Test
    void assignToCourseDelegatesToService() throws Exception {
        when(registrationServices.assignRegistrationToCourse(10L, 3L)).thenReturn(registration(10L));

        mockMvc.perform(put("/registration/assignToCourse/10/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numRegistration").value(10));

        verify(registrationServices).assignRegistrationToCourse(10L, 3L);
    }

    @Test
    void addAndAssignToSkierAndCourseDelegatesToService() throws Exception {
        when(registrationServices.addRegistrationAndAssignToSkierAndCourse(any(Registration.class), eq(1L), eq(3L)))
                .thenReturn(registration(10L));

        mockMvc.perform(put("/registration/addAndAssignToSkierAndCourse/1/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numRegistration").value(10));

        verify(registrationServices).addRegistrationAndAssignToSkierAndCourse(any(Registration.class), eq(1L), eq(3L));
    }

    @Test
    void numWeeksReturnsWeeksForInstructorAndSupport() throws Exception {
        when(registrationServices.numWeeksCourseOfInstructorBySupport(2L, Support.SKI)).thenReturn(Arrays.asList(3, 4, 9));

        mockMvc.perform(get("/registration/numWeeks/2/SKI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2]").value(9));
    }

    @Test
    void numWeeksRejectsUnknownSupport() throws Exception {
        mockMvc.perform(get("/registration/numWeeks/2/SURF"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationServices);
    }
}

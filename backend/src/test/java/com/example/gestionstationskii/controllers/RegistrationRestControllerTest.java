package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IRegistrationServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationRestControllerTest {
    @Mock private IRegistrationServices service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new RegistrationRestController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void postRegistersSkierToSlotAndReturnsCreatedEntity() throws Exception {
        Registration registration = new Registration();
        registration.setNumRegistration(8L);
        registration.setNumWeek(42);
        when(service.registerSlot(3L, 5L)).thenReturn(registration);

        mvc.perform(post("/registration/skier/3/slot/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numRegistration").value(8))
                .andExpect(jsonPath("$.numWeek").value(42))
                .andExpect(jsonPath("$.cancelled").value(false));
        verify(service).registerSlot(3L, 5L);
    }

    @Test
    void registrationConflictIsReturnedAsConflictJson() throws Exception {
        when(service.registerSlot(3L, 5L)).thenThrow(BusinessException.conflict("Capacité atteinte"));

        mvc.perform(post("/registration/skier/3/slot/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Capacité atteinte"))
                .andExpect(jsonPath("$.path").value("/registration/skier/3/slot/5"));
    }

    @Test
    void skierHistoryReturnsEmptyJsonArrayWhenThereIsNoHistory() throws Exception {
        when(service.bySkier(3L)).thenReturn(Collections.<Registration>emptyList());

        mvc.perform(get("/registration/skier/3"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).bySkier(3L);
    }

    @Test
    void searchBindsIsoDatesAndOptionalCourseId() throws Exception {
        when(service.search(any(LocalDateTime.class), any(LocalDateTime.class), eq(12L)))
                .thenReturn(Collections.<Registration>emptyList());

        mvc.perform(get("/registration")
                        .param("from", "2026-10-19T09:00:00")
                        .param("to", "2026-10-19T10:00:00")
                        .param("courseId", "12"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(service).search(LocalDateTime.parse("2026-10-19T09:00:00"),
                LocalDateTime.parse("2026-10-19T10:00:00"), 12L);
    }

    @Test
    void searchRejectsMissingOrMalformedDateParameters() throws Exception {
        mvc.perform(get("/registration").param("from", "2026-10-19T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Requête invalide : vérifier les champs requis, les types et les dates ISO"));
        mvc.perform(get("/registration")
                        .param("from", "tomorrow")
                        .param("to", "2026-10-20T10:00:00"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deleteCancelsRegistrationAndReturnsNoContentBody() throws Exception {
        mvc.perform(delete("/registration/8"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        verify(service).cancel(8L);
    }

    @Test
    void addAndAssignmentRoutesBindTheirBodiesAndPathIdentifiers() throws Exception {
        Registration created = new Registration();
        created.setNumRegistration(21L);
        when(service.addRegistrationAndAssignToSkier(any(Registration.class), eq(3L))).thenReturn(created);
        when(service.assignRegistrationToCourse(21L, 12L)).thenReturn(created);
        when(service.addRegistrationAndAssignToSkierAndCourse(any(Registration.class), eq(3L), eq(12L)))
                .thenReturn(created);

        mvc.perform(put("/registration/addAndAssignToSkier/3")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numWeek\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numRegistration").value(21));
        mvc.perform(put("/registration/assignToCourse/21/12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numRegistration").value(21));
        mvc.perform(put("/registration/addAndAssignToSkierAndCourse/3/12")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numRegistration").value(21));

        verify(service).addRegistrationAndAssignToSkier(any(Registration.class), eq(3L));
        verify(service).assignRegistrationToCourse(21L, 12L);
        verify(service).addRegistrationAndAssignToSkierAndCourse(any(Registration.class), eq(3L), eq(12L));
    }

    @Test
    void instructorWeeksRouteConvertsSupportAndRejectsUnknownValues() throws Exception {
        when(service.numWeeksCourseOfInstructorBySupport(4L, Support.SKI)).thenReturn(Arrays.asList(2, 5));

        mvc.perform(get("/registration/numWeeks/4/SKI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2))
                .andExpect(jsonPath("$[1]").value(5));
        mvc.perform(get("/registration/numWeeks/4/NOT_A_SUPPORT"))
                .andExpect(status().isBadRequest());

        verify(service).numWeeksCourseOfInstructorBySupport(4L, Support.SKI);
    }
}

package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.ISubscriptionServices;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionRestControllerTest {

    private static final String SUBSCRIPTION_JSON = "{\"startDate\":\"2030-01-01\",\"endDate\":\"2030-12-31\","
            + "\"price\":300,\"typeSub\":\"ANNUAL\"}";

    @Mock
    private ISubscriptionServices subscriptionServices;

    @InjectMocks
    private SubscriptionRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static Subscription subscription(Long id, TypeSubscription type) {
        Subscription s = new Subscription();
        s.setNumSub(id);
        s.setTypeSub(type);
        s.setPrice(300f);
        return s;
    }

    @Test
    void addSubscriptionConvertsPayloadAndDelegates() throws Exception {
        when(subscriptionServices.addSubscription(any(Subscription.class)))
                .thenReturn(subscription(4L, TypeSubscription.ANNUAL));

        mockMvc.perform(post("/subscription/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBSCRIPTION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSub").value(4))
                .andExpect(jsonPath("$.typeSub").value("ANNUAL"));

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionServices).addSubscription(captor.capture());
        assertEquals(LocalDate.of(2030, 1, 1), captor.getValue().getStartDate());
        assertEquals(LocalDate.of(2030, 12, 31), captor.getValue().getEndDate());
        assertEquals(Float.valueOf(300f), captor.getValue().getPrice());
        assertEquals(TypeSubscription.ANNUAL, captor.getValue().getTypeSub());
    }

    @Test
    void addSubscriptionMapsValidationErrors() throws Exception {
        when(subscriptionServices.addSubscription(any(Subscription.class)))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "Dates invalides"));

        mockMvc.perform(post("/subscription/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBSCRIPTION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dates invalides"));
    }

    @Test
    void addSubscriptionRejectsMalformedDate() throws Exception {
        mockMvc.perform(post("/subscription/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"01/01/2030\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subscriptionServices);
    }

    @Test
    void getByIdReturnsSubscription() throws Exception {
        when(subscriptionServices.retrieveSubscriptionById(4L)).thenReturn(subscription(4L, TypeSubscription.MONTHLY));

        mockMvc.perform(get("/subscription/get/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSub").value(4))
                .andExpect(jsonPath("$.typeSub").value("MONTHLY"));
    }

    @Test
    void getByIdMapsMissingSubscriptionToNotFound() throws Exception {
        when(subscriptionServices.retrieveSubscriptionById(9L)).thenThrow(BusinessException.missing("Abonnement"));

        mockMvc.perform(get("/subscription/get/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Abonnement introuvable"));
    }

    @Test
    void getByTypeReturnsSubscriptionsOfThatType() throws Exception {
        when(subscriptionServices.getSubscriptionByType(TypeSubscription.MONTHLY))
                .thenReturn(new HashSet<>(Collections.singleton(subscription(4L, TypeSubscription.MONTHLY))));

        mockMvc.perform(get("/subscription/all/MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].typeSub").value("MONTHLY"));
    }

    @Test
    void getByTypeRejectsUnknownType() throws Exception {
        mockMvc.perform(get("/subscription/all/WEEKLY"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subscriptionServices);
    }

    @Test
    void updateConvertsPayloadAndDelegates() throws Exception {
        when(subscriptionServices.updateSubscription(any(Subscription.class)))
                .thenReturn(subscription(4L, TypeSubscription.SEMESTRIEL));

        mockMvc.perform(put("/subscription/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numSub\":4,\"startDate\":\"2030-01-01\",\"endDate\":\"2030-06-30\",\"price\":180,\"typeSub\":\"SEMESTRIEL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeSub").value("SEMESTRIEL"));

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionServices).updateSubscription(captor.capture());
        assertEquals(Long.valueOf(4L), captor.getValue().getNumSub());
        assertEquals(TypeSubscription.SEMESTRIEL, captor.getValue().getTypeSub());
    }

    @Test
    void getByDatesParsesIsoDatesAndDelegates() throws Exception {
        LocalDate d1 = LocalDate.of(2030, 1, 1);
        LocalDate d2 = LocalDate.of(2030, 12, 31);
        when(subscriptionServices.retrieveSubscriptionsByDates(d1, d2))
                .thenReturn(Arrays.asList(subscription(1L, TypeSubscription.ANNUAL), subscription(2L, TypeSubscription.MONTHLY)));

        mockMvc.perform(get("/subscription/all/2030-01-01/2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(subscriptionServices).retrieveSubscriptionsByDates(d1, d2);
    }

    @Test
    void getByDatesRejectsInvalidDates() throws Exception {
        mockMvc.perform(get("/subscription/all/2030-01-01/pas-une-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subscriptionServices);
    }

    @Test
    void statusReturnsComputedStatusForGivenDate() throws Exception {
        when(subscriptionServices.status(4L, LocalDate.of(2030, 6, 1))).thenReturn("ACTIVE");

        mockMvc.perform(get("/subscription/4/status").param("asOf", "2030-06-01"))
                .andExpect(status().isOk())
                .andExpect(content().string("ACTIVE"));

        verify(subscriptionServices).status(4L, LocalDate.of(2030, 6, 1));
    }

    @Test
    void statusRequiresAsOfDate() throws Exception {
        mockMvc.perform(get("/subscription/4/status"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subscriptionServices);
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/subscription/delete/4"))
                .andExpect(status().isOk());

        verify(subscriptionServices).removeSubscription(4L);
    }

    @Test
    void deleteMapsConflictWhenSubscriptionIsInUse() throws Exception {
        doThrow(BusinessException.conflict("Abonnement utilisé")).when(subscriptionServices).removeSubscription(4L);

        mockMvc.perform(delete("/subscription/delete/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Abonnement utilisé"));
    }
}

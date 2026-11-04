package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.ISkierServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkierRestControllerTest {

    private static final String SKIER_JSON = "{\"firstName\":\"Sahar\",\"lastName\":\"Saidani\",\"city\":\"Tunis\","
            + "\"dateOfBirth\":\"2000-01-01\"}";
    private static final String SKIER_WITH_SUBSCRIPTION_JSON = "{\"firstName\":\"Sahar\",\"lastName\":\"Saidani\","
            + "\"city\":\"Tunis\",\"dateOfBirth\":\"2000-01-01\","
            + "\"subscription\":{\"typeSub\":\"ANNUAL\",\"price\":120,\"startDate\":\"2030-01-01\",\"endDate\":\"2030-12-31\"}}";

    @Mock
    private ISkierServices skierServices;

    @InjectMocks
    private SkierRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static Skier skier(Long id) {
        Skier s = new Skier();
        s.setNumSkier(id);
        s.setFirstName("Sahar");
        s.setLastName("Saidani");
        s.setCity("Tunis");
        s.setDateOfBirth(LocalDate.of(2000, 1, 1));
        return s;
    }

    @Test
    void getAllReturnsSkiers() throws Exception {
        when(skierServices.retrieveAllSkiers()).thenReturn(Arrays.asList(skier(1L), skier(2L)));

        mockMvc.perform(get("/skier/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numSkier").value(1))
                .andExpect(jsonPath("$[1].firstName").value("Sahar"));
    }

    @Test
    void getByIdReturnsSkier() throws Exception {
        when(skierServices.retrieveSkier(1L)).thenReturn(skier(1L));

        mockMvc.perform(get("/skier/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(1))
                .andExpect(jsonPath("$.city").value("Tunis"));
    }

    @Test
    void getByIdMapsMissingSkierToNotFound() throws Exception {
        when(skierServices.retrieveSkier(9L)).thenThrow(BusinessException.missing("Skieur"));

        mockMvc.perform(get("/skier/get/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Skieur introuvable"))
                .andExpect(jsonPath("$.path").value("/skier/get/9"));
    }

    @Test
    void getByIdRejectsNonNumericIdentifier() throws Exception {
        mockMvc.perform(get("/skier/get/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(skierServices);
    }

    @Test
    void addSkierConvertsPayloadToEntity() throws Exception {
        when(skierServices.addSkier(any(Skier.class))).thenReturn(skier(5L));

        mockMvc.perform(post("/skier/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SKIER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(5));

        ArgumentCaptor<Skier> captor = ArgumentCaptor.forClass(Skier.class);
        verify(skierServices).addSkier(captor.capture());
        assertEquals("Sahar", captor.getValue().getFirstName());
        assertEquals(LocalDate.of(2000, 1, 1), captor.getValue().getDateOfBirth());
        assertNull(captor.getValue().getSubscription());
    }

    @Test
    void addSkierConvertsNestedSubscription() throws Exception {
        when(skierServices.addSkier(any(Skier.class))).thenReturn(skier(5L));

        mockMvc.perform(post("/skier/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SKIER_WITH_SUBSCRIPTION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Skier> captor = ArgumentCaptor.forClass(Skier.class);
        verify(skierServices).addSkier(captor.capture());
        assertNotNull(captor.getValue().getSubscription());
        assertEquals(TypeSubscription.ANNUAL, captor.getValue().getSubscription().getTypeSub());
        assertEquals(Float.valueOf(120f), captor.getValue().getSubscription().getPrice());
    }

    @Test
    void addSkierRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/skier/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(skierServices);
    }

    @Test
    void addSkierRejectsUnknownSubscriptionType() throws Exception {
        mockMvc.perform(post("/skier/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"subscription\":{\"typeSub\":\"WEEKLY\"}}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(skierServices);
    }

    @Test
    void addAndAssignDelegatesWithCourseIdentifier() throws Exception {
        when(skierServices.addSkierAndAssignToCourse(any(Skier.class), eq(3L))).thenReturn(skier(6L));

        mockMvc.perform(post("/skier/addAndAssign/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SKIER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(6));

        verify(skierServices).addSkierAndAssignToCourse(any(Skier.class), eq(3L));
    }

    @Test
    void assignToSubscriptionDelegatesToService() throws Exception {
        when(skierServices.assignSkierToSubscription(1L, 2L)).thenReturn(skier(1L));

        mockMvc.perform(put("/skier/assignToSub/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(1));

        verify(skierServices).assignSkierToSubscription(1L, 2L);
    }

    @Test
    void assignToSubscriptionMapsConflict() throws Exception {
        when(skierServices.assignSkierToSubscription(1L, 2L))
                .thenThrow(BusinessException.conflict("Abonnement déjà affecté"));

        mockMvc.perform(put("/skier/assignToSub/1/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Abonnement déjà affecté"));
    }

    @Test
    void assignToPisteDelegatesToService() throws Exception {
        when(skierServices.assignSkierToPiste(1L, 4L)).thenReturn(skier(1L));

        mockMvc.perform(put("/skier/assignToPiste/1/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(1));

        verify(skierServices).assignSkierToPiste(1L, 4L);
    }

    @Test
    void getSkiersBySubscriptionFiltersOnType() throws Exception {
        when(skierServices.retrieveSkiersBySubscriptionType(TypeSubscription.MONTHLY))
                .thenReturn(Collections.singletonList(skier(1L)));

        mockMvc.perform(get("/skier/getSkiersBySubscription").param("typeSubscription", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getSkiersBySubscriptionRejectsMissingOrUnknownType() throws Exception {
        mockMvc.perform(get("/skier/getSkiersBySubscription"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/skier/getSkiersBySubscription").param("typeSubscription", "WEEKLY"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(skierServices);
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/skier/delete/1"))
                .andExpect(status().isOk());

        verify(skierServices).removeSkier(1L);
    }

    @Test
    void deleteMapsConflictWhenSkierHasHistory() throws Exception {
        doThrow(BusinessException.conflict("Historique")).when(skierServices).removeSkier(1L);

        mockMvc.perform(delete("/skier/delete/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateDelegatesToService() throws Exception {
        when(skierServices.updateSkier(any(Skier.class))).thenReturn(skier(1L));

        mockMvc.perform(put("/skier/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numSkier\":1,\"firstName\":\"Sahar\",\"lastName\":\"Saidani\",\"city\":\"Tunis\",\"dateOfBirth\":\"2000-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSkier").value(1));

        ArgumentCaptor<Skier> captor = ArgumentCaptor.forClass(Skier.class);
        verify(skierServices).updateSkier(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getNumSkier());
    }

    @Test
    void searchUsesDefaultsWhenNoParametersAreGiven() throws Exception {
        Page<Skier> page = new PageImpl<>(Collections.singletonList(skier(1L)), PageRequest.of(0, 20), 1);
        when(skierServices.search(anyString(), anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/skier/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(skierServices).search("", "", 0, 20);
    }

    @Test
    void searchForwardsExplicitParameters() throws Exception {
        Page<Skier> page = new PageImpl<>(Collections.<Skier>emptyList(), PageRequest.of(2, 5), 0);
        when(skierServices.search(anyString(), anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/skier/search")
                        .param("name", "sa")
                        .param("city", "tun")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        verify(skierServices).search("sa", "tun", 2, 5);
    }

    @Test
    void searchMapsInvalidPaginationToBadRequest() throws Exception {
        when(skierServices.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "Pagination invalide"));

        mockMvc.perform(get("/skier/search").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pagination invalide"));
    }
}

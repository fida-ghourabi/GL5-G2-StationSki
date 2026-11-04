package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPisteServices;
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
class PisteRestControllerTest {

    private static final String PISTE_JSON =
            "{\"namePiste\":\"Aigle\",\"color\":\"RED\",\"length\":1500,\"slope\":30}";

    @Mock
    private IPisteServices pisteServices;

    @InjectMocks
    private PisteRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static Piste piste(Long id, Color color) {
        Piste p = new Piste();
        p.setNumPiste(id);
        p.setNamePiste("Aigle");
        p.setColor(color);
        p.setLength(1500);
        p.setSlope(30);
        return p;
    }

    @Test
    void getAllReturnsPistes() throws Exception {
        when(pisteServices.retrieveAllPistes()).thenReturn(Arrays.asList(piste(1L, Color.RED), piste(2L, Color.BLUE)));

        mockMvc.perform(get("/piste/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].color").value("RED"))
                .andExpect(jsonPath("$[1].numPiste").value(2));
    }

    @Test
    void getByIdReturnsPiste() throws Exception {
        when(pisteServices.retrievePiste(1L)).thenReturn(piste(1L, Color.RED));

        mockMvc.perform(get("/piste/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.namePiste").value("Aigle"))
                .andExpect(jsonPath("$.length").value(1500))
                .andExpect(jsonPath("$.slope").value(30));
    }

    @Test
    void getByIdMapsMissingPisteToNotFound() throws Exception {
        when(pisteServices.retrievePiste(9L)).thenThrow(BusinessException.missing("Piste"));

        mockMvc.perform(get("/piste/get/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Piste introuvable"));
    }

    @Test
    void addPisteConvertsPayloadToEntity() throws Exception {
        when(pisteServices.addPiste(any(Piste.class))).thenReturn(piste(7L, Color.RED));

        mockMvc.perform(post("/piste/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PISTE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numPiste").value(7));

        ArgumentCaptor<Piste> captor = ArgumentCaptor.forClass(Piste.class);
        verify(pisteServices).addPiste(captor.capture());
        assertEquals("Aigle", captor.getValue().getNamePiste());
        assertEquals(Color.RED, captor.getValue().getColor());
        assertEquals(1500, captor.getValue().getLength());
        assertEquals(30, captor.getValue().getSlope());
    }

    @Test
    void addPisteRejectsUnknownColor() throws Exception {
        mockMvc.perform(post("/piste/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"namePiste\":\"X\",\"color\":\"PURPLE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pisteServices);
    }

    @Test
    void addPisteMapsBusinessValidationErrors() throws Exception {
        when(pisteServices.addPiste(any(Piste.class)))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "Pente invalide"));

        mockMvc.perform(post("/piste/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PISTE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pente invalide"));
    }

    @Test
    void updateDelegatesToService() throws Exception {
        when(pisteServices.updatePiste(any(Piste.class))).thenReturn(piste(1L, Color.BLACK));

        mockMvc.perform(put("/piste/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPiste\":1,\"namePiste\":\"Aigle\",\"color\":\"BLACK\",\"length\":1500,\"slope\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("BLACK"));

        ArgumentCaptor<Piste> captor = ArgumentCaptor.forClass(Piste.class);
        verify(pisteServices).updatePiste(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getNumPiste());
        assertEquals(Color.BLACK, captor.getValue().getColor());
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        mockMvc.perform(delete("/piste/delete/1"))
                .andExpect(status().isOk());

        verify(pisteServices).removePiste(1L);
    }

    @Test
    void deleteMapsConflictWhenSkiersAreAssigned() throws Exception {
        doThrow(BusinessException.conflict("Piste affectée")).when(pisteServices).removePiste(1L);

        mockMvc.perform(delete("/piste/delete/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Piste affectée"));
    }

    @Test
    void searchWithoutColorPassesNull() throws Exception {
        when(pisteServices.search(null)).thenReturn(Arrays.asList(piste(1L, Color.RED), piste(2L, Color.BLUE)));

        mockMvc.perform(get("/piste/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(pisteServices).search(null);
    }

    @Test
    void searchWithColorPassesColor() throws Exception {
        when(pisteServices.search(Color.RED)).thenReturn(Collections.singletonList(piste(1L, Color.RED)));

        mockMvc.perform(get("/piste/search").param("color", "RED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].color").value("RED"));
    }

    @Test
    void searchRejectsUnknownColor() throws Exception {
        mockMvc.perform(get("/piste/search").param("color", "PURPLE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pisteServices);
    }

    @Test
    void skiersReturnsSkiersOfPiste() throws Exception {
        Skier s = new Skier();
        s.setNumSkier(3L);
        s.setFirstName("Sahar");
        when(pisteServices.skiers(1L)).thenReturn(new HashSet<>(Collections.singleton(s)));

        mockMvc.perform(get("/piste/1/skiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numSkier").value(3));
    }
}

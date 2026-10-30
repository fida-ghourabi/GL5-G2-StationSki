package com.example.gestionstationskii.services.piste;

import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPisteServices;
import com.example.gestionstationskii.services.PisteServicesImpl;
import com.example.gestionstationskii.controllers.PisteRestController;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.repositories.IPisteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated CRUD unit tests: real service, mocked repository, no Spring or database. */
@ExtendWith(MockitoExtension.class)
class PisteCrudMockitoTest {
    @Mock private IPisteRepository repository;
    @Mock private IPisteServices controllerService;
    @InjectMocks private PisteServicesImpl service;

    private Piste piste() {
        Piste piste = new Piste();
        piste.setNamePiste("Test piste");
        piste.setColor(Color.BLUE);
        piste.setLength(1200);
        piste.setSlope(20);
        piste.setSkiers(new HashSet<>());
        return piste;
    }

    @Test
    void createPiste() {
        Piste input = piste();
        Piste saved = piste();
        saved.setNumPiste(1L);
        when(repository.save(input)).thenReturn(saved);

        Piste result = service.addPiste(input);

        assertSame(saved, result);
        assertEquals(1L, result.getNumPiste());
        assertTrue(input.getSkiers().isEmpty());
        verify(repository).save(input);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void readPiste() {
        Piste stored = piste();
        stored.setNumPiste(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.findAll()).thenReturn(Collections.singletonList(stored));

        assertSame(stored, service.retrievePiste(1L));
        assertEquals(Collections.singletonList(stored), service.retrieveAllPistes());

        verify(repository).findById(1L);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updatePiste() {
        Piste stored = piste();
        stored.setNumPiste(1L);
        Piste changes = piste();
        changes.setNumPiste(1L);
        changes.setNamePiste("Updated piste");
        changes.setColor(Color.RED);
        changes.setLength(1800);
        changes.setSlope(35);
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.save(stored)).thenReturn(stored);

        Piste result = service.updatePiste(changes);

        assertSame(stored, result);
        assertEquals(1L, result.getNumPiste());
        assertEquals("Updated piste", result.getNamePiste());
        assertEquals(Color.RED, result.getColor());
        assertEquals(1800, result.getLength());
        assertEquals(35, result.getSlope());
        verify(repository).findById(1L);
        verify(repository).save(stored);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deletePiste() {
        Piste stored = piste();
        stored.setNumPiste(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(stored));

        service.removePiste(1L);

        verify(repository).findById(1L);
        verify(repository).delete(stored);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createPisteControllerMapsDtoToEntity() {
        Requests.PisteInput input = pisteInput();
        when(controllerService.addPiste(any(Piste.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Piste result = new PisteRestController(controllerService).addPiste(input);

        assertEquals(input.getNamePiste(), result.getNamePiste());
        assertEquals(input.getColor(), result.getColor());
        assertEquals(input.getLength(), result.getLength());
        assertEquals(input.getSlope(), result.getSlope());
        verify(controllerService).addPiste(argThat(piste ->
                piste.getNumPiste() == null
                        && input.getNamePiste().equals(piste.getNamePiste())
                        && input.getColor() == piste.getColor()
                        && input.getLength() == piste.getLength()
                        && input.getSlope() == piste.getSlope()));
        verifyNoMoreInteractions(controllerService);
    }

    @Test
    void updatePisteControllerMapsDtoToEntity() {
        Requests.PisteInput input = pisteInput();
        input.setNumPiste(7L);
        input.setNamePiste("Updated via DTO");
        when(controllerService.updatePiste(any(Piste.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Piste result = new PisteRestController(controllerService).update(input);

        assertEquals(7L, result.getNumPiste());
        assertEquals("Updated via DTO", result.getNamePiste());
        verify(controllerService).updatePiste(argThat(piste ->
                piste.getNumPiste().equals(7L)
                        && "Updated via DTO".equals(piste.getNamePiste())
                        && input.getColor() == piste.getColor()
                        && input.getLength() == piste.getLength()
                        && input.getSlope() == piste.getSlope()));
        verifyNoMoreInteractions(controllerService);
    }

    private Requests.PisteInput pisteInput() {
        Requests.PisteInput input = new Requests.PisteInput();
        input.setNamePiste("Test piste");
        input.setColor(Color.BLUE);
        input.setLength(1200);
        input.setSlope(20);
        return input;
    }
}

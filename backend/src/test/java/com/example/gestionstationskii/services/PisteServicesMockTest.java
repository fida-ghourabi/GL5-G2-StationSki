package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.repositories.IPisteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.gestionstationskii.services.BusinessAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PisteServicesMockTest {

    @Mock
    private IPisteRepository pisteRepository;

    @InjectMocks
    private PisteServicesImpl service;

    private static Piste piste(Long id, Color color) {
        Piste p = new Piste();
        p.setNumPiste(id);
        p.setNamePiste("Piste " + id);
        p.setColor(color);
        p.setLength(1200);
        p.setSlope(25);
        p.setSkiers(new HashSet<>());
        return p;
    }

    // ---------- retrieve ----------

    @Test
    void retrieveAllPistesReturnsRepositoryContent() {
        when(pisteRepository.findAll()).thenReturn(Arrays.asList(piste(1L, Color.RED), piste(2L, Color.BLUE)));

        assertEquals(2, service.retrieveAllPistes().size());
    }

    @Test
    void retrievePisteReturnsExistingPiste() {
        Piste p = piste(1L, Color.RED);
        when(pisteRepository.findById(1L)).thenReturn(Optional.of(p));

        assertSame(p, service.retrievePiste(1L));
    }

    @Test
    void retrievePisteFailsWhenMissing() {
        when(pisteRepository.findById(1L)).thenReturn(Optional.<Piste>empty());

        assertNotFound(() -> service.retrievePiste(1L));
    }

    @Test
    void retrievePisteRejectsInvalidIdentifier() {
        assertBadRequest(() -> service.retrievePiste(null));
        assertBadRequest(() -> service.retrievePiste(0L));
        verifyNoInteractions(pisteRepository);
    }

    // ---------- addPiste ----------

    @Test
    void addPisteSavesValidPisteWithEmptySkierSet() {
        Piste input = piste(null, Color.GREEN);
        input.setSkiers(null);
        when(pisteRepository.save(any(Piste.class))).thenAnswer(inv -> inv.getArgument(0));

        Piste saved = service.addPiste(input);

        assertNotNull(saved.getSkiers());
        assertTrue(saved.getSkiers().isEmpty());
        verify(pisteRepository).save(input);
    }

    @Test
    void addPisteRejectsPresetIdentifier() {
        assertBadRequest(() -> service.addPiste(piste(3L, Color.GREEN)));
        verify(pisteRepository, never()).save(any(Piste.class));
    }

    @Test
    void addPisteRejectsInvalidFields() {
        Piste noName = piste(null, Color.RED);
        noName.setNamePiste(" ");
        assertBadRequest(() -> service.addPiste(noName));

        Piste noColor = piste(null, null);
        assertBadRequest(() -> service.addPiste(noColor));

        Piste zeroLength = piste(null, Color.RED);
        zeroLength.setLength(0);
        assertBadRequest(() -> service.addPiste(zeroLength));

        Piste negativeSlope = piste(null, Color.RED);
        negativeSlope.setSlope(-1);
        assertBadRequest(() -> service.addPiste(negativeSlope));

        Piste verticalPlus = piste(null, Color.RED);
        verticalPlus.setSlope(91);
        assertBadRequest(() -> service.addPiste(verticalPlus));

        verify(pisteRepository, never()).save(any(Piste.class));
    }

    @Test
    void addPisteAcceptsSlopeBoundaries() {
        when(pisteRepository.save(any(Piste.class))).thenAnswer(inv -> inv.getArgument(0));

        Piste flat = piste(null, Color.GREEN);
        flat.setSlope(0);
        assertEquals(0, service.addPiste(flat).getSlope());

        Piste steep = piste(null, Color.BLACK);
        steep.setSlope(90);
        assertEquals(90, service.addPiste(steep).getSlope());
    }

    // ---------- removePiste ----------

    @Test
    void removePisteDeletesPisteWithoutSkiers() {
        Piste p = piste(1L, Color.RED);
        when(pisteRepository.findById(1L)).thenReturn(Optional.of(p));

        service.removePiste(1L);

        verify(pisteRepository).delete(p);
    }

    @Test
    void removePisteRefusesWhenSkiersAreAssigned() {
        Piste p = piste(1L, Color.RED);
        p.setSkiers(new HashSet<>(Collections.singleton(new Skier())));
        when(pisteRepository.findById(1L)).thenReturn(Optional.of(p));

        assertConflict(() -> service.removePiste(1L));
        verify(pisteRepository, never()).delete(any(Piste.class));
    }

    @Test
    void removePisteFailsWhenMissing() {
        when(pisteRepository.findById(1L)).thenReturn(Optional.<Piste>empty());

        assertNotFound(() -> service.removePiste(1L));
    }

    // ---------- updatePiste ----------

    @Test
    void updatePisteCopiesEditableFields() {
        Piste current = piste(1L, Color.GREEN);
        Piste input = piste(1L, Color.BLACK);
        input.setNamePiste("Diable");
        input.setLength(3000);
        input.setSlope(40);
        when(pisteRepository.findById(1L)).thenReturn(Optional.of(current));
        when(pisteRepository.save(current)).thenReturn(current);

        Piste result = service.updatePiste(input);

        assertEquals("Diable", result.getNamePiste());
        assertEquals(Color.BLACK, result.getColor());
        assertEquals(3000, result.getLength());
        assertEquals(40, result.getSlope());
    }

    @Test
    void updatePisteRejectsInvalidInputBeforeAnyLookup() {
        Piste invalid = piste(1L, null);

        assertBadRequest(() -> service.updatePiste(invalid));
        verify(pisteRepository, never()).findById(any(Long.class));
    }

    @Test
    void updatePisteFailsWhenMissing() {
        when(pisteRepository.findById(1L)).thenReturn(Optional.<Piste>empty());

        assertNotFound(() -> service.updatePiste(piste(1L, Color.RED)));
    }

    @Test
    void updatePisteRejectsMissingIdentifier() {
        assertBadRequest(() -> service.updatePiste(piste(null, Color.RED)));
    }

    // ---------- search / skiers ----------

    @Test
    void searchWithoutColorReturnsEveryPiste() {
        when(pisteRepository.findAll()).thenReturn(Arrays.asList(
                piste(1L, Color.RED), piste(2L, Color.BLUE), piste(3L, Color.RED)));

        assertEquals(3, service.search(null).size());
    }

    @Test
    void searchWithColorFiltersPistes() {
        when(pisteRepository.findAll()).thenReturn(Arrays.asList(
                piste(1L, Color.RED), piste(2L, Color.BLUE), piste(3L, Color.RED)));

        List<Piste> reds = service.search(Color.RED);

        assertEquals(2, reds.size());
        for (Piste p : reds) assertEquals(Color.RED, p.getColor());
        assertTrue(service.search(Color.BLACK).isEmpty());
    }

    @Test
    void skiersReturnsSkiersOfPiste() {
        Piste p = piste(1L, Color.RED);
        Skier s = new Skier();
        p.setSkiers(new HashSet<>(Collections.singleton(s)));
        when(pisteRepository.findById(1L)).thenReturn(Optional.of(p));

        Set<Skier> result = service.skiers(1L);

        assertEquals(1, result.size());
        assertTrue(result.contains(s));
    }

    @Test
    void skiersFailsWhenPisteMissing() {
        when(pisteRepository.findById(1L)).thenReturn(Optional.<Piste>empty());

        assertNotFound(() -> service.skiers(1L));
    }
}

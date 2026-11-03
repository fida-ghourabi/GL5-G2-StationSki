package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.services.IPisteServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PisteRestControllerTest {
    @Mock private IPisteServices services;
    private PisteRestController controller;

    @BeforeEach
    void setUp() { controller = new PisteRestController(services); }

    @Test
    void delegatesPisteEndpointsAndMapsRequestDto() {
        Long id = 7L;
        Piste piste = new Piste();
        List<Piste> pistes = List.of(piste);
        Set<Skier> skiers = Set.of(new Skier());
        when(services.updatePiste(any(Piste.class))).thenAnswer(call -> call.getArgument(0));
        when(services.addPiste(any(Piste.class))).thenAnswer(call -> call.getArgument(0));
        when(services.search(Color.BLUE)).thenReturn(pistes);
        when(services.skiers(id)).thenReturn(skiers);
        when(services.retrieveAllPistes()).thenReturn(pistes);
        when(services.retrievePiste(id)).thenReturn(piste);

        Requests.PisteInput input = new Requests.PisteInput();
        input.setNumPiste(id);
        input.setNamePiste("Alpine");
        input.setColor(Color.BLUE);
        input.setLength(1200);
        input.setSlope(25);

        assertSame(pistes, controller.getAllPistes());
        assertSame(piste, controller.getById(id));
        org.junit.jupiter.api.Assertions.assertEquals("Alpine", controller.addPiste(input).getNamePiste());
        org.junit.jupiter.api.Assertions.assertEquals("Alpine", controller.update(input).getNamePiste());
        assertSame(pistes, controller.search(Color.BLUE));
        assertSame(skiers, controller.skiers(id));
        controller.deleteById(id);

        verify(services).addPiste(argThat(value -> value.getNumPiste().equals(id)
                && value.getNamePiste().equals("Alpine") && value.getColor() == Color.BLUE));
        verify(services).removePiste(id);
    }
}

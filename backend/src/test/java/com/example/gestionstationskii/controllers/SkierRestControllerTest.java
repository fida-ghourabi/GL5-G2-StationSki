package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.services.ISkierServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkierRestControllerTest {
    @Mock private ISkierServices services;
    private SkierRestController controller;

    @BeforeEach
    void setUp() { controller = new SkierRestController(services); }

    @Test
    void delegatesSkierEndpointsAndMapsRequestDto() {
        Long id = 9L;
        Long relatedId = 4L;
        Skier skier = new Skier();
        List<Skier> skiers = List.of(skier);
        Page<Skier> page = Page.empty();
        when(services.updateSkier(any(Skier.class))).thenAnswer(call -> call.getArgument(0));
        when(services.addSkier(any(Skier.class))).thenAnswer(call -> call.getArgument(0));
        when(services.addSkierAndAssignToCourse(any(Skier.class), org.mockito.ArgumentMatchers.eq(relatedId)))
                .thenAnswer(call -> call.getArgument(0));
        when(services.assignSkierToSubscription(id, relatedId)).thenReturn(skier);
        when(services.assignSkierToPiste(id, relatedId)).thenReturn(skier);
        when(services.retrieveSkiersBySubscriptionType(TypeSubscription.ANNUAL)).thenReturn(skiers);
        when(services.retrieveSkier(id)).thenReturn(skier);
        when(services.retrieveAllSkiers()).thenReturn(skiers);
        when(services.search("Fida", "Tunis", 1, 10)).thenReturn(page);

        Requests.SkierInput input = new Requests.SkierInput();
        input.setNumSkier(id);
        input.setFirstName("Fida");
        input.setLastName("Ghourabi");
        input.setDateOfBirth(LocalDate.of(2000, 5, 10));
        input.setCity("Tunis");

        assertSame(skiers, controller.getAllSkiers());
        assertSame(skier, controller.getById(id));
        org.junit.jupiter.api.Assertions.assertEquals("Fida", controller.addSkier(input).getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Fida", controller.update(input).getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Fida", controller.addSkierAndAssignToCourse(input, relatedId).getFirstName());
        assertSame(skier, controller.assignToSubscription(id, relatedId));
        assertSame(skier, controller.assignToPiste(id, relatedId));
        assertSame(skiers, controller.retrieveSkiersBySubscriptionType(TypeSubscription.ANNUAL));
        assertSame(page, controller.search("Fida", "Tunis", 1, 10));
        controller.deleteById(id);

        verify(services).addSkier(argThat(value -> value.getNumSkier().equals(id)
                && value.getFirstName().equals("Fida") && value.getCity().equals("Tunis")));
        verify(services).removeSkier(id);
    }
}

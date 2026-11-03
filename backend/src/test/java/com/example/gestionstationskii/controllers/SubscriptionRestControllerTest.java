package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.services.ISubscriptionServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionRestControllerTest {
    @Mock private ISubscriptionServices services;
    private SubscriptionRestController controller;

    @BeforeEach
    void setUp() { controller = new SubscriptionRestController(services); }

    @Test
    void delegatesSubscriptionEndpointsAndMapsRequestDto() {
        Long id = 6L;
        LocalDate date = LocalDate.of(2026, 10, 31);
        Subscription subscription = new Subscription();
        Set<Subscription> subscriptions = Set.of(subscription);
        List<Subscription> byDates = List.of(subscription);
        when(services.status(id, date)).thenReturn("ACTIVE");
        when(services.retrieveSubscriptionById(id)).thenReturn(subscription);
        when(services.getSubscriptionByType(TypeSubscription.ANNUAL)).thenReturn(subscriptions);
        when(services.retrieveSubscriptionsByDates(date, date)).thenReturn(byDates);
        when(services.addSubscription(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));
        when(services.updateSubscription(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));

        Requests.SubscriptionInput input = new Requests.SubscriptionInput();
        input.setNumSub(id);
        input.setTypeSub(TypeSubscription.ANNUAL);
        input.setStartDate(date);
        input.setEndDate(date.plusYears(1));
        input.setPrice(250F);

        assertEquals("ACTIVE", controller.status(id, date));
        assertSame(subscription, controller.getById(id));
        assertEquals(id, controller.addSubscription(input).getNumSub());
        assertEquals(id, controller.updateSubscription(input).getNumSub());
        assertSame(subscriptions, controller.getSubscriptionsByType(TypeSubscription.ANNUAL));
        assertSame(byDates, controller.getSubscriptionsByDates(date, date));
        controller.delete(id);

        verify(services).addSubscription(argThat(value -> value.getNumSub().equals(id)
                && value.getTypeSub() == TypeSubscription.ANNUAL && value.getPrice().equals(250F)));
        verify(services).removeSubscription(id);
    }
}

package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.repositories.ISkierRepository;
import com.example.gestionstationskii.repositories.ISubscriptionRepository;
import com.example.gestionstationskii.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServicesMockitoTest {

    private ISubscriptionRepository subscriptionRepository;
    private ISkierRepository skierRepository;
    private PaymentRepository paymentRepository;
    private SubscriptionServicesImpl service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(ISubscriptionRepository.class);
        skierRepository = mock(ISkierRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        service = new SubscriptionServicesImpl(
                subscriptionRepository,
                skierRepository,
                paymentRepository
        );
    }

    @Test
    void addSubscriptionCalculatesAnnualEndDate() {
        Subscription subscription = subscription(null, TypeSubscription.ANNUAL);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        assertSame(subscription, service.addSubscription(subscription));

        assertEquals(LocalDate.of(2027, 1, 31), subscription.getEndDate());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void addSubscriptionCalculatesMonthlyEndDateAtMonthEnd() {
        Subscription subscription = subscription(null, TypeSubscription.MONTHLY);
        subscription.setStartDate(LocalDate.of(2028, 1, 31));
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        service.addSubscription(subscription);

        assertEquals(LocalDate.of(2028, 2, 29), subscription.getEndDate());
    }

    @Test
    void addSubscriptionCalculatesSemesterEndDate() {
        Subscription subscription = subscription(null, TypeSubscription.SEMESTRIEL);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        service.addSubscription(subscription);

        assertEquals(LocalDate.of(2026, 7, 31), subscription.getEndDate());
    }

    @Test
    void addSubscriptionRejectsProvidedIdentifier() {
        Subscription subscription = subscription(10L, TypeSubscription.MONTHLY);

        assertThrows(BusinessException.class, () -> service.addSubscription(subscription));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void addSubscriptionRejectsMissingType() {
        Subscription subscription = subscription(null, null);

        assertThrows(BusinessException.class, () -> service.addSubscription(subscription));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void addSubscriptionRejectsMissingStartDate() {
        Subscription subscription = new Subscription();
        subscription.setPrice(50F);
        subscription.setTypeSub(TypeSubscription.MONTHLY);

        assertThrows(BusinessException.class, () -> service.addSubscription(subscription));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void addSubscriptionRejectsMissingPrice() {
        Subscription subscription = subscription(null, TypeSubscription.MONTHLY);
        subscription.setPrice(null);

        assertThrows(BusinessException.class, () -> service.addSubscription(subscription));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void addSubscriptionRejectsNegativePrice() {
        Subscription subscription = subscription(null, TypeSubscription.MONTHLY);
        subscription.setPrice(-1F);

        assertThrows(BusinessException.class, () -> service.addSubscription(subscription));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void addSubscriptionRejectsNonFinitePrices() {
        Subscription nanPrice = subscription(null, TypeSubscription.MONTHLY);
        nanPrice.setPrice(Float.NaN);
        Subscription infinitePrice = subscription(null, TypeSubscription.MONTHLY);
        infinitePrice.setPrice(Float.POSITIVE_INFINITY);

        assertThrows(BusinessException.class, () -> service.addSubscription(nanPrice));
        assertThrows(BusinessException.class, () -> service.addSubscription(infinitePrice));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void updateSubscriptionCopiesValuesAndRecalculatesEndDate() {
        Subscription current = subscription(1L, TypeSubscription.ANNUAL);
        Subscription update = subscription(1L, TypeSubscription.MONTHLY);
        update.setStartDate(LocalDate.of(2026, 2, 28));
        update.setEndDate(LocalDate.of(2099, 1, 1));

        when(subscriptionRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(paymentRepository.existsBySubscriptionNumSub(1L)).thenReturn(false);
        when(subscriptionRepository.save(current)).thenReturn(current);

        Subscription result = service.updateSubscription(update);

        assertSame(current, result);
        assertEquals(TypeSubscription.MONTHLY, current.getTypeSub());
        assertEquals(LocalDate.of(2026, 2, 28), current.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 28), current.getEndDate());
        assertEquals(update.getPrice(), current.getPrice());
        verify(subscriptionRepository).save(current);
    }

    @Test
    void updateSubscriptionRejectsInvalidIdentifier() {
        Subscription update = subscription(null, TypeSubscription.MONTHLY);

        assertThrows(BusinessException.class, () -> service.updateSubscription(update));

        verifyNoInteractions(subscriptionRepository, paymentRepository);
    }

    @Test
    void updateSubscriptionRejectsMissingSubscription() {
        Subscription update = subscription(99L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.lockById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.updateSubscription(update));

        verify(paymentRepository, never()).existsBySubscriptionNumSub(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void updateSubscriptionRejectsSubscriptionWithPayment() {
        Subscription current = subscription(1L, TypeSubscription.MONTHLY);
        Subscription update = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(paymentRepository.existsBySubscriptionNumSub(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.updateSubscription(update));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void retrieveSubscriptionByIdReturnsSubscription() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        assertSame(subscription, service.retrieveSubscriptionById(1L));
    }

    @Test
    void retrieveSubscriptionByIdRejectsInvalidIdentifier() {
        assertThrows(BusinessException.class, () -> service.retrieveSubscriptionById(0L));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void retrieveSubscriptionByIdRejectsMissingSubscription() {
        when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.retrieveSubscriptionById(99L));
    }

    @Test
    void getSubscriptionByTypeDelegatesToRepository() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.findByTypeSubOrderByStartDateAsc(TypeSubscription.MONTHLY))
                .thenReturn(Collections.singleton(subscription));

        assertEquals(
                Collections.singleton(subscription),
                service.getSubscriptionByType(TypeSubscription.MONTHLY)
        );
    }

    @Test
    void getSubscriptionByTypeRejectsNullType() {
        assertThrows(BusinessException.class, () -> service.getSubscriptionByType(null));

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void retrieveSubscriptionsByDatesDelegatesToRepository() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 1);
        when(subscriptionRepository.getSubscriptionsByStartDateBetween(start, end))
                .thenReturn(Collections.emptyList());

        assertEquals(
                Collections.emptyList(),
                service.retrieveSubscriptionsByDates(start, end)
        );
    }

    @Test
    void retrieveSubscriptionsByDatesRejectsMissingStartDate() {
        LocalDate endDate = LocalDate.now();
        assertThrows(
                BusinessException.class,
                () -> service.retrieveSubscriptionsByDates(null, endDate)
        );
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void retrieveSubscriptionsByDatesRejectsMissingEndDate() {
        LocalDate startDate = LocalDate.now();
        assertThrows(
                BusinessException.class,
                () -> service.retrieveSubscriptionsByDates(startDate, null)
        );
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void retrieveSubscriptionsByDatesRejectsReversedPeriod() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 1);
        assertThrows(
                BusinessException.class,
                () -> service.retrieveSubscriptionsByDates(startDate, endDate)
        );
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void retrieveSubscriptionsLogsOnlySubscriptionsWithSkier() {
        Subscription withoutSkier = subscription(1L, TypeSubscription.MONTHLY);
        Subscription withSkier = subscription(2L, TypeSubscription.ANNUAL);
        Skier skier = new Skier();
        skier.setFirstName("Amina");
        skier.setLastName("Test");

        when(subscriptionRepository.findDistinctOrderByEndDateAsc())
                .thenReturn(Arrays.asList(withoutSkier, withSkier));
        when(skierRepository.findBySubscription(withoutSkier)).thenReturn(null);
        when(skierRepository.findBySubscription(withSkier)).thenReturn(skier);

        service.retrieveSubscriptions();

        verify(skierRepository).findBySubscription(withoutSkier);
        verify(skierRepository).findBySubscription(withSkier);
    }

    @Test
    void removeSubscriptionDeletesUnusedSubscription() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.lockById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.existsBySubscriptionNumSub(1L)).thenReturn(false);
        when(skierRepository.findBySubscription(subscription)).thenReturn(null);

        service.removeSubscription(1L);

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void removeSubscriptionRejectsInvalidIdentifier() {
        assertThrows(BusinessException.class, () -> service.removeSubscription(0L));

        verifyNoInteractions(subscriptionRepository, paymentRepository, skierRepository);
    }

    @Test
    void removeSubscriptionRejectsMissingSubscription() {
        when(subscriptionRepository.lockById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.removeSubscription(99L));

        verify(paymentRepository, never()).existsBySubscriptionNumSub(any());
    }

    @Test
    void removeSubscriptionRejectsSubscriptionWithPayment() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.lockById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.existsBySubscriptionNumSub(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.removeSubscription(1L));

        verify(skierRepository, never()).findBySubscription(any());
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void removeSubscriptionRejectsSubscriptionAssignedToSkier() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        when(subscriptionRepository.lockById(1L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.existsBySubscriptionNumSub(1L)).thenReturn(false);
        when(skierRepository.findBySubscription(subscription)).thenReturn(new Skier());

        assertThrows(BusinessException.class, () -> service.removeSubscription(1L));

        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void statusReturnsUpcoming() {
        givenSubscriptionForStatus();

        assertEquals("UPCOMING", service.status(1L, LocalDate.of(2025, 12, 31)));
    }

    @Test
    void statusReturnsActive() {
        givenSubscriptionForStatus();

        assertEquals("ACTIVE", service.status(1L, LocalDate.of(2026, 6, 1)));
    }

    @Test
    void statusReturnsExpiredOnEndDate() {
        givenSubscriptionForStatus();

        assertEquals("EXPIRED", service.status(1L, LocalDate.of(2027, 1, 1)));
    }

    @Test
    void statusRejectsMissingReferenceDate() {
        givenSubscriptionForStatus();

        assertThrows(BusinessException.class, () -> service.status(1L, null));
    }

    @Test
    void statusRejectsSubscriptionWithMissingStartDate() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        subscription.setStartDate(null);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        LocalDate referenceDate = LocalDate.now();

        assertThrows(BusinessException.class, () -> service.status(1L, referenceDate));
    }

    @Test
    void statusRejectsSubscriptionWithMissingEndDate() {
        Subscription subscription = subscription(1L, TypeSubscription.MONTHLY);
        subscription.setEndDate(null);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        LocalDate referenceDate = LocalDate.now();

        assertThrows(BusinessException.class, () -> service.status(1L, referenceDate));
    }

    private static Subscription subscription(Long id, TypeSubscription type) {
        Subscription subscription = new Subscription();
        subscription.setNumSub(id);
        subscription.setTypeSub(type);
        subscription.setStartDate(LocalDate.of(2026, 1, 31));
        subscription.setEndDate(LocalDate.of(2026, 2, 28));
        subscription.setPrice(50F);
        return subscription;
    }

    private void givenSubscriptionForStatus() {
        Subscription subscription = subscription(1L, TypeSubscription.ANNUAL);
        subscription.setStartDate(LocalDate.of(2026, 1, 1));
        subscription.setEndDate(LocalDate.of(2027, 1, 1));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
    }
}

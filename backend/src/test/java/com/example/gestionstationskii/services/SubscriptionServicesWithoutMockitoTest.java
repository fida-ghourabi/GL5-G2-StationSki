package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.repositories.ISubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionServicesWithoutMockitoTest {

    @Autowired
    private ISubscriptionServices subscriptionService;

    @Autowired
    private ISubscriptionRepository subscriptionRepository;

    @BeforeEach
    void cleanDatabase() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void addSubscriptionPersistsSubscriptionAndCalculatesEndDate() {
        Subscription subscription = new Subscription();
        subscription.setTypeSub(TypeSubscription.MONTHLY);
        subscription.setStartDate(LocalDate.of(2026, 1, 31));
        subscription.setPrice(50F);

        Subscription saved = subscriptionService.addSubscription(subscription);

        assertNotNull(saved.getNumSub());
        assertEquals(LocalDate.of(2026, 2, 28), saved.getEndDate());
        assertTrue(subscriptionRepository.findById(saved.getNumSub()).isPresent());
    }

    @Test
    void findSubscriptionsByTypeUsesRealRepository() {
        Subscription monthly = createSubscription(
                TypeSubscription.MONTHLY,
                LocalDate.of(2026, 1, 1)
        );
        Subscription annual = createSubscription(
                TypeSubscription.ANNUAL,
                LocalDate.of(2026, 1, 1)
        );

        subscriptionService.addSubscription(monthly);
        subscriptionService.addSubscription(annual);

        assertEquals(
                1,
                subscriptionService
                        .getSubscriptionByType(TypeSubscription.MONTHLY)
                        .size()
        );
    }

    @Test
    void findSubscriptionsByDatesUsesRealRepository() {
        Subscription included = createSubscription(
                TypeSubscription.MONTHLY,
                LocalDate.of(2026, 3, 15)
        );
        Subscription excluded = createSubscription(
                TypeSubscription.MONTHLY,
                LocalDate.of(2026, 8, 15)
        );

        subscriptionService.addSubscription(included);
        subscriptionService.addSubscription(excluded);

        assertEquals(
                1,
                subscriptionService.retrieveSubscriptionsByDates(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 4, 1)
                ).size()
        );
    }

    @Test
    void retrieveAndRemoveSubscriptionUseRealRepository() {
        Subscription subscription = createSubscription(
                TypeSubscription.MONTHLY,
                LocalDate.of(2026, 5, 1)
        );
        Subscription saved = subscriptionService.addSubscription(subscription);

        assertEquals(
                saved.getNumSub(),
                subscriptionService
                        .retrieveSubscriptionById(saved.getNumSub())
                        .getNumSub()
        );

        subscriptionService.removeSubscription(saved.getNumSub());

        assertTrue(subscriptionRepository.findById(saved.getNumSub()).isEmpty());
    }

    @Test
    void statusUsesPersistedSubscriptionDates() {
        Subscription subscription = createSubscription(
                TypeSubscription.ANNUAL,
                LocalDate.of(2026, 1, 1)
        );
        Subscription saved = subscriptionService.addSubscription(subscription);

        assertEquals(
                "UPCOMING",
                subscriptionService.status(
                        saved.getNumSub(),
                        LocalDate.of(2025, 12, 31)
                )
        );
        assertEquals(
                "ACTIVE",
                subscriptionService.status(
                        saved.getNumSub(),
                        LocalDate.of(2026, 6, 1)
                )
        );
        assertEquals(
                "EXPIRED",
                subscriptionService.status(
                        saved.getNumSub(),
                        LocalDate.of(2027, 1, 1)
                )
        );
    }

    private Subscription createSubscription(
            TypeSubscription type,
            LocalDate startDate
    ) {
        Subscription subscription = new Subscription();
        subscription.setTypeSub(type);
        subscription.setStartDate(startDate);
        subscription.setPrice(50F);
        return subscription;
    }
}

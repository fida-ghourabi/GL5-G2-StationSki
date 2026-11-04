package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.TypeSubscription;
import com.example.gestionstationskii.repositories.ISubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Transactional
class SubscriptionServicesWithoutMockitoTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("station_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private ISubscriptionServices subscriptionService;

    @Autowired
    private ISubscriptionRepository subscriptionRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add(
                "spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.MySQL8Dialect"
        );
    }

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

        // Java 8 : Optional.isEmpty() n'existe pas (Java 11+)
        assertFalse(subscriptionRepository.findById(saved.getNumSub()).isPresent());
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
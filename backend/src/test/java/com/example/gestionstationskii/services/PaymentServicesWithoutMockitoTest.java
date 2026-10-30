package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class PaymentServicesWithoutMockitoTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("station_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private IPaymentServices paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private ISkierRepository skierRepository;

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
        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        skierRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    void recordsPaymentAndReadsItBySkierAndSubscription() {

        TestData data = createTestData();

        Requests.PaymentInput input = paymentInput(
                data.skier.getNumSkier(),
                data.subscription.getNumSub()
        );

        Payment saved = paymentService.recordPayment(input);

        assertNotNull(saved.getId());
        assertEquals(PaymentStatus.PENDING, saved.getStatus());

        assertEquals(
                1,
                paymentService.bySkier(data.skier.getNumSkier()).size()
        );

        assertEquals(
                1,
                paymentService.bySubscription(data.subscription.getNumSub()).size()
        );
    }

    @Test
    void changesStatusAndCreatesRefund() {

        TestData data = createTestData();

        Payment payment = paymentService.recordPayment(
                paymentInput(
                        data.skier.getNumSkier(),
                        data.subscription.getNumSub()
                )
        );

        Requests.PaymentState state = new Requests.PaymentState();
        state.setStatus(PaymentStatus.PAID);

        paymentService.changeStatus(payment.getId(), state);

        Requests.RefundInput refund = new Requests.RefundInput();
        refund.setAmount(new BigDecimal("25.00"));
        refund.setReason("Customer request");

        paymentService.refund(payment.getId(), refund);

        Payment persistedPayment = paymentRepository
                .findById(payment.getId())
                .orElseThrow(() -> new AssertionError(
                        "Le paiement devrait exister dans la base de données"
                ));

        assertEquals(
                PaymentStatus.PARTIALLY_REFUNDED,
                persistedPayment.getStatus()
        );

        assertEquals(
                1,
                paymentService.refunds(payment.getId()).size()
        );
    }

    @Test
    void buildsReceiptFromPersistedPayment() {

        TestData data = createTestData();

        Payment payment = paymentService.recordPayment(
                paymentInput(
                        data.skier.getNumSkier(),
                        data.subscription.getNumSub()
                )
        );

        assertEquals(
                payment.getReceiptNumber(),
                paymentService.receipt(payment.getId()).getReceiptNumber()
        );
    }

    private TestData createTestData() {

        Subscription subscription = new Subscription();

        subscription.setTypeSub(TypeSubscription.MONTHLY);
        subscription.setStartDate(java.time.LocalDate.of(2026, 1, 1));
        subscription.setPrice(50F);

        subscription = subscriptionRepository.save(subscription);

        Skier skier = new Skier();

        skier.setFirstName("Amina");
        skier.setLastName("Test");
        skier.setSubscription(subscription);

        skier = skierRepository.save(skier);

        return new TestData(skier, subscription);
    }

    private static Requests.PaymentInput paymentInput(
            Long skierId,
            Long subscriptionId
    ) {

        Requests.PaymentInput input = new Requests.PaymentInput();

        input.setSkierId(skierId);
        input.setSubscriptionId(subscriptionId);
        input.setAmount(new BigDecimal("100.00"));
        input.setMethod(PaymentMethod.CARD);

        return input;
    }

    private static class TestData {

        private final Skier skier;
        private final Subscription subscription;

        private TestData(Skier skier, Subscription subscription) {
            this.skier = skier;
            this.subscription = subscription;
        }
    }
}

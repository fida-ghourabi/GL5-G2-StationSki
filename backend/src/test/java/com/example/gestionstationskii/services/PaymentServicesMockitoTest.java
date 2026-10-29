package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServicesMockitoTest {

    private PaymentRepository payments;
    private RefundRepository refunds;
    private ISkierRepository skiers;
    private ISubscriptionRepository subscriptions;
    private PaymentServicesImpl service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        refunds = mock(RefundRepository.class);
        skiers = mock(ISkierRepository.class);
        subscriptions = mock(ISubscriptionRepository.class);
        service = new PaymentServicesImpl(payments, refunds, skiers, subscriptions);
    }

    @Test
    void recordCreatesPendingPaymentForMatchingSkierSubscription() {
        Skier skier = skier(1L);
        Subscription subscription = subscription(2L);
        skier.setSubscription(subscription);
        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        input.setMethod(PaymentMethod.CARD);
        input.setTransactionReference("TX-1");

        when(skiers.lockById(1L)).thenReturn(Optional.of(skier));
        when(subscriptions.lockById(2L)).thenReturn(Optional.of(subscription));
        when(payments.existsByTransactionReference("TX-1")).thenReturn(false);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });

        Payment result = service.recordPayment(input);

        assertEquals(10L, result.getId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("TX-1", result.getTransactionReference());
        assertNotNull(result.getReceiptNumber());
        verify(payments).save(any(Payment.class));
    }

    @Test
    void recordRejectsMissingPaymentMethod() {
        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        input.setMethod(null);

        assertThrows(BusinessException.class, () -> service.recordPayment(input));

        verifyNoInteractions(skiers, subscriptions, payments);
    }

    @Test
    void recordRejectsNonPositiveAmount() {
        Requests.PaymentInput zero = paymentInput(1L, 2L, "0.00");
        Requests.PaymentInput negative = paymentInput(1L, 2L, "-1.00");

        assertThrows(BusinessException.class, () -> service.recordPayment(zero));
        assertThrows(BusinessException.class, () -> service.recordPayment(negative));

        verifyNoInteractions(skiers, subscriptions, payments);
    }

    @Test
    void recordRejectsMissingSkier() {
        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        when(skiers.lockById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.recordPayment(input));

        verify(subscriptions, never()).lockById(any());
    }

    @Test
    void recordRejectsMissingSubscription() {
        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier(1L)));
        when(subscriptions.lockById(2L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.recordPayment(input));

        verify(payments, never()).save(any());
    }

    @Test
    void recordRejectsSubscriptionNotAssignedToSkier() {
        Skier skier = skier(1L);
        skier.setSubscription(subscription(99L));
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier));
        when(subscriptions.lockById(2L)).thenReturn(Optional.of(subscription(2L)));

        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");

        assertThrows(BusinessException.class, () -> service.recordPayment(input));

        verify(payments, never()).save(any());
    }

    @Test
    void recordRejectsDuplicateTransactionReference() {
        Skier skier = skier(1L);
        Subscription subscription = subscription(2L);
        skier.setSubscription(subscription);
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier));
        when(subscriptions.lockById(2L)).thenReturn(Optional.of(subscription));
        when(payments.existsByTransactionReference("TX-1")).thenReturn(true);

        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        input.setTransactionReference("TX-1");

        assertThrows(BusinessException.class, () -> service.recordPayment(input));

        verify(payments, never()).save(any());
    }

    @Test
    void recordRejectsInvalidTransactionReference() {
        Skier skier = skier(1L);
        Subscription subscription = subscription(2L);
        skier.setSubscription(subscription);
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier));
        when(subscriptions.lockById(2L)).thenReturn(Optional.of(subscription));

        Requests.PaymentInput input = paymentInput(1L, 2L, "100.00");
        input.setTransactionReference("x".repeat(256));

        assertThrows(BusinessException.class, () -> service.recordPayment(input));
        verify(payments, never()).save(any());
    }

    @Test
    void getReturnsPayment() {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        when(payments.findById(1L)).thenReturn(Optional.of(payment));

        assertSame(payment, service.get(1L));
    }

    @Test
    void getRejectsInvalidOrMissingPayment() {
        assertThrows(BusinessException.class, () -> service.get(0L));
        when(payments.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.get(99L));
    }

    @Test
    void changeStatusMovesPendingPaymentToPaid() {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        Requests.PaymentState input = new Requests.PaymentState();
        input.setStatus(PaymentStatus.PAID);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        Payment result = service.changeStatus(1L, input);

        assertSame(payment, result);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertNotNull(payment.getPaidAt());
    }

    @Test
    void changeStatusMovesPendingPaymentToFailed() {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        Requests.PaymentState input = new Requests.PaymentState();
        input.setStatus(PaymentStatus.FAILED);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        service.changeStatus(1L, input);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNull(payment.getPaidAt());
    }

    @Test
    void changeStatusReturnsPaymentWhenStatusIsUnchanged() {
        Payment payment = payment(1L, PaymentStatus.PAID);
        Requests.PaymentState input = new Requests.PaymentState();
        input.setStatus(PaymentStatus.PAID);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        assertSame(payment, service.changeStatus(1L, input));

        verify(payments).lockById(1L);
    }

    @Test
    void changeStatusRejectsMissingStatus() {
        Requests.PaymentState input = new Requests.PaymentState();

        assertThrows(BusinessException.class, () -> service.changeStatus(1L, input));

        verifyNoInteractions(payments);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        Payment payment = payment(1L, PaymentStatus.PAID);
        Requests.PaymentState input = new Requests.PaymentState();
        input.setStatus(PaymentStatus.FAILED);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BusinessException.class, () -> service.changeStatus(1L, input));
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void refundCreatesPartialRefund() {
        Payment payment = payment(1L, PaymentStatus.PAID);
        Requests.RefundInput input = refundInput("25.00", "Correction");
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));
        when(refunds.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund result = service.refund(1L, input);

        assertEquals(new BigDecimal("25.00"), result.getAmount());
        assertEquals(new BigDecimal("25.00"), payment.getRefundedAmount());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
    }

    @Test
    void refundCreatesFullRefund() {
        Payment payment = payment(1L, PaymentStatus.PAID);
        Requests.RefundInput input = refundInput("100.00", "Full correction");
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));
        when(refunds.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.refund(1L, input);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(new BigDecimal("100.00"), payment.getRefundedAmount());
    }

    @Test
    void refundRejectsUnpaidPayment() {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        Requests.RefundInput input = refundInput("10.00", "Correction");

        assertThrows(BusinessException.class, () -> service.refund(1L, input));

        verify(refunds, never()).save(any());
    }

    @Test
    void refundRejectsAmountAboveRemainingBalance() {
        Payment payment = payment(1L, PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("75.00"));
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));

        Requests.RefundInput input = refundInput("25.01", "Correction");

        assertThrows(BusinessException.class, () -> service.refund(1L, input));

        verify(refunds, never()).save(any());
    }

    @Test
    void refundRejectsInvalidInput() {
        Payment payment = payment(1L, PaymentStatus.PAID);
        when(payments.lockById(1L)).thenReturn(Optional.of(payment));
        Requests.RefundInput missingReason = refundInput("10.00", "");
        Requests.RefundInput negativeAmount = refundInput("-1.00", "Correction");

        assertThrows(BusinessException.class, () -> service.refund(1L, missingReason));
        assertThrows(BusinessException.class, () -> service.refund(1L, negativeAmount));
        verify(payments, never()).lockById(any());
    }

    @Test
    void bySkierAndBySubscriptionValidateOwnerResources() {
        Payment payment = payment(1L, PaymentStatus.PENDING);
        when(skiers.findById(1L)).thenReturn(Optional.of(skier(1L)));
        when(subscriptions.findById(2L)).thenReturn(Optional.of(subscription(2L)));
        when(payments.findBySkier_NumSkier(1L)).thenReturn(Collections.singletonList(payment));
        when(payments.findBySubscription_NumSub(2L)).thenReturn(Collections.singletonList(payment));

        assertEquals(1, service.bySkier(1L).size());
        assertEquals(1, service.bySubscription(2L).size());
    }

    @Test
    void refundsReturnsPaymentRefunds() {
        when(payments.findById(1L)).thenReturn(Optional.of(payment(1L, PaymentStatus.PAID)));
        when(refunds.findByPayment_Id(1L)).thenReturn(Collections.emptyList());

        assertTrue(service.refunds(1L).isEmpty());
    }

    @Test
    void receiptBuildsReceiptAndCalculatesNetAmount() {
        Skier skier = skier(2L);
        Subscription subscription = subscription(3L);
        Payment payment = payment(1L, PaymentStatus.PARTIALLY_REFUNDED);
        payment.setSkier(skier);
        payment.setSubscription(subscription);
        payment.setReceiptNumber("SKI-TEST");
        payment.setPaymentDate(LocalDateTime.of(2026, 1, 15, 10, 0));
        payment.setPaidAt(LocalDateTime.of(2026, 1, 15, 10, 1));
        payment.setRefundedAmount(new BigDecimal("25.00"));
        when(payments.findById(1L)).thenReturn(Optional.of(payment));
        when(refunds.findByPayment_Id(1L)).thenReturn(Collections.emptyList());

        PaymentReceipt receipt = service.receipt(1L);

        assertEquals("SKI-TEST", receipt.getReceiptNumber());
        assertEquals(new BigDecimal("75.00"), receipt.getNetAmount());
    }

    private static Requests.PaymentInput paymentInput(Long skierId, Long subscriptionId, String amount) {
        Requests.PaymentInput input = new Requests.PaymentInput();
        input.setSkierId(skierId);
        input.setSubscriptionId(subscriptionId);
        input.setAmount(new BigDecimal(amount));
        input.setMethod(PaymentMethod.CASH);
        return input;
    }

    private static Requests.RefundInput refundInput(String amount, String reason) {
        Requests.RefundInput input = new Requests.RefundInput();
        input.setAmount(new BigDecimal(amount));
        input.setReason(reason);
        return input;
    }

    private static Skier skier(Long id) {
        Skier skier = new Skier();
        skier.setNumSkier(id);
        skier.setFirstName("Amina");
        skier.setLastName("Test");
        return skier;
    }

    private static Subscription subscription(Long id) {
        Subscription subscription = new Subscription();
        subscription.setNumSub(id);
        return subscription;
    }

    private static Payment payment(Long id, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(status);
        payment.setSkier(skier(2L));
        payment.setSubscription(subscription(3L));
        payment.setReceiptNumber("SKI-TEST");
        payment.setMethod(PaymentMethod.CARD);
        return payment;
    }
}

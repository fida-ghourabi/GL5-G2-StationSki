package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Payment;
import com.example.gestionstationskii.entities.PaymentStatus;
import com.example.gestionstationskii.entities.Refund;
import com.example.gestionstationskii.services.IPaymentServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRestControllerTest {
    @Mock private IPaymentServices services;
    private PaymentRestController controller;

    @BeforeEach
    void setUp() { controller = new PaymentRestController(services); }

    @Test
    void delegatesEveryPaymentEndpoint() {
        Long id = 5L;
        Payment payment = new Payment();
        Refund refund = new Refund();
        PaymentReceipt receipt = mock(PaymentReceipt.class);
        List<Payment> payments = List.of(payment);
        List<Refund> refunds = List.of(refund);
        Requests.PaymentInput input = new Requests.PaymentInput();
        input.setSkierId(2L);
        input.setSubscriptionId(3L);
        Requests.PaymentState state = new Requests.PaymentState();
        state.setStatus(PaymentStatus.PAID);
        Requests.RefundInput refundInput = new Requests.RefundInput();

        when(services.recordPayment(input)).thenReturn(payment);
        when(services.get(id)).thenReturn(payment);
        when(services.bySkier(2L)).thenReturn(payments);
        when(services.bySubscription(3L)).thenReturn(payments);
        when(services.changeStatus(id, state)).thenReturn(payment);
        when(services.refund(id, refundInput)).thenReturn(refund);
        when(services.refunds(id)).thenReturn(refunds);
        when(services.receipt(id)).thenReturn(receipt);

        assertSame(payment, controller.recordPayment(input));
        assertSame(payment, controller.get(id));
        assertSame(payments, controller.bySkier(2L));
        assertSame(payments, controller.bySubscription(3L));
        assertSame(payment, controller.status(id, state));
        assertSame(refund, controller.refund(id, refundInput));
        assertSame(refunds, controller.refunds(id));
        assertSame(receipt, controller.receipt(id));

        verify(services).recordPayment(input);
        verify(services).changeStatus(id, state);
        verify(services).refund(id, refundInput);
    }
}

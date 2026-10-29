package com.example.gestionstationskii.services;

import com.example.gestionstationskii.controllers.PaymentRestController;
import com.example.gestionstationskii.controllers.SubscriptionRestController;
import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP contracts with real JSON binding and isolated service dependencies. */
@ExtendWith(MockitoExtension.class)
class ControllerCoverageTest {
    @Mock private IPaymentServices payments;
    @Mock private ISubscriptionServices subscriptions;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PaymentRestController(payments),
                new SubscriptionRestController(subscriptions)).build();
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setId(7L);
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Subscription subscription() {
        Subscription subscription = new Subscription();
        subscription.setNumSub(8L);
        return subscription;
    }

    @Test
    void recordsPaymentWithJsonCommand() throws Exception {
        when(payments.record(any())).thenReturn(payment());
        mvc.perform(post("/payments").contentType("application/json")
                .content("{\"skierId\":2,\"subscriptionId\":8,\"amount\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(payments).record(argThat(input -> input.getSkierId().equals(2L)
                && input.getSubscriptionId().equals(8L) && input.getAmount().intValue() == 50));
    }

    @Test
    void retrievesPaymentAndFilteredLists() throws Exception {
        when(payments.get(7L)).thenReturn(payment());
        when(payments.bySkier(2L)).thenReturn(Collections.singletonList(payment()));
        when(payments.bySubscription(8L)).thenReturn(Collections.singletonList(payment()));
        mvc.perform(get("/payments/7")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        mvc.perform(get("/payments/skier/2")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        mvc.perform(get("/payments/subscription/8")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        verify(payments).get(7L);
        verify(payments).bySkier(2L);
        verify(payments).bySubscription(8L);
    }

    @Test
    void updatesPaymentStatus() throws Exception {
        Payment paid = payment();
        paid.setStatus(PaymentStatus.PAID);
        when(payments.changeStatus(eq(7L), any())).thenReturn(paid);
        mvc.perform(put("/payments/7/status").contentType("application/json").content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        verify(payments).changeStatus(eq(7L), argThat(input -> input.getStatus() == PaymentStatus.PAID));
    }

    @Test
    void recordsAndListsRefunds() throws Exception {
        Refund refund = new Refund();
        refund.setId(9L);
        when(payments.refund(eq(7L), any())).thenReturn(refund);
        when(payments.refunds(7L)).thenReturn(Collections.singletonList(refund));
        mvc.perform(post("/payments/7/refunds").contentType("application/json")
                .content("{\"amount\":10,\"reason\":\"Cancellation\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9));
        mvc.perform(get("/payments/7/refunds")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(9));
        verify(payments).refund(eq(7L), argThat(input -> input.getAmount().intValue() == 10
                && "Cancellation".equals(input.getReason())));
        verify(payments).refunds(7L);
    }

    @Test
    void returnsReceipt() throws Exception {
        PaymentReceipt receipt = new PaymentReceipt("TEST-7", 7L, 2L, "Test", 8L,
                null, null, null, PaymentStatus.PAID, java.math.BigDecimal.TEN,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.TEN, Collections.emptyList());
        when(payments.receipt(7L)).thenReturn(receipt);
        mvc.perform(get("/payments/7/receipt")).andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("TEST-7"));
        verify(payments).receipt(7L);
    }

    @Test
    void createsSubscriptionAndMapsDates() throws Exception {
        when(subscriptions.addSubscription(any())).thenReturn(subscription());
        mvc.perform(post("/subscription/add").contentType("application/json")
                .content("{\"startDate\":\"2030-01-01\",\"price\":50,\"typeSub\":\"MONTHLY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numSub").value(8));
        verify(subscriptions).addSubscription(argThat(input -> input.getNumSub() == null
                && input.getStartDate().equals(LocalDate.of(2030, 1, 1))
                && input.getPrice().equals(50f) && input.getTypeSub() == TypeSubscription.MONTHLY));
    }

    @Test
    void updatesSubscription() throws Exception {
        when(subscriptions.updateSubscription(any())).thenReturn(subscription());
        mvc.perform(put("/subscription/update").contentType("application/json")
                .content("{\"numSub\":8,\"price\":75,\"typeSub\":\"MONTHLY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numSub").value(8));
        verify(subscriptions).updateSubscription(argThat(input -> input.getNumSub().equals(8L)
                && input.getPrice().equals(75f)));
    }

    @Test
    void retrievesSubscriptionsByIdTypeAndDateRange() throws Exception {
        LocalDate from = LocalDate.of(2030, 1, 1);
        LocalDate to = from.plusMonths(1);
        when(subscriptions.retrieveSubscriptionById(8L)).thenReturn(subscription());
        when(subscriptions.getSubscriptionByType(TypeSubscription.MONTHLY)).thenReturn(Collections.singleton(subscription()));
        when(subscriptions.retrieveSubscriptionsByDates(from, to)).thenReturn(Collections.singletonList(subscription()));
        mvc.perform(get("/subscription/get/8")).andExpect(status().isOk()).andExpect(jsonPath("$.numSub").value(8));
        mvc.perform(get("/subscription/all/MONTHLY")).andExpect(status().isOk()).andExpect(jsonPath("$[0].numSub").value(8));
        mvc.perform(get("/subscription/all/2030-01-01/2030-02-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numSub").value(8));
        verify(subscriptions).retrieveSubscriptionById(8L);
        verify(subscriptions).getSubscriptionByType(TypeSubscription.MONTHLY);
        verify(subscriptions).retrieveSubscriptionsByDates(from, to);
    }

    @Test
    void calculatesStatusAndDeletesSubscription() throws Exception {
        LocalDate date = LocalDate.of(2030, 1, 1);
        when(subscriptions.status(8L, date)).thenReturn("ACTIVE");
        mvc.perform(get("/subscription/8/status").param("asOf", "2030-01-01"))
                .andExpect(status().isOk()).andExpect(content().string("ACTIVE"));
        mvc.perform(delete("/subscription/delete/8")).andExpect(status().isOk());
        verify(subscriptions).status(8L, date);
        verify(subscriptions).removeSubscription(8L);
    }

    @Test
    void rejectsMalformedRequestsBeforeServices() throws Exception {
        mvc.perform(get("/subscription/8/status").param("asOf", "not-a-date")).andExpect(status().isBadRequest());
        mvc.perform(post("/payments").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payments, subscriptions);
    }
}

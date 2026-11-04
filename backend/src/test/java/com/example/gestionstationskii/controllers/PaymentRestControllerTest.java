package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Payment;
import com.example.gestionstationskii.entities.PaymentMethod;
import com.example.gestionstationskii.entities.PaymentStatus;
import com.example.gestionstationskii.entities.Refund;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPaymentServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentRestControllerTest {

    private static final String PAYMENT_JSON = "{\"skierId\":1,\"subscriptionId\":2,\"amount\":150.00,"
            + "\"method\":\"CARD\",\"transactionReference\":\"TX-1\"}";

    @Mock
    private IPaymentServices service;

    @InjectMocks
    private PaymentRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static Payment payment(Long id, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(id);
        p.setAmount(new BigDecimal("150.00"));
        p.setMethod(PaymentMethod.CARD);
        p.setStatus(status);
        p.setReceiptNumber("R-" + id);
        return p;
    }

    private static Refund refund(Long id) {
        Refund r = new Refund();
        r.setId(id);
        r.setAmount(new BigDecimal("20.00"));
        r.setReason("Erreur");
        return r;
    }

    @Test
    void recordPaymentConvertsPayloadAndDelegates() throws Exception {
        when(service.recordPayment(any(Requests.PaymentInput.class))).thenReturn(payment(5L, PaymentStatus.PENDING));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.receiptNumber").value("R-5"));

        ArgumentCaptor<Requests.PaymentInput> captor = ArgumentCaptor.forClass(Requests.PaymentInput.class);
        verify(service).recordPayment(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getSkierId());
        assertEquals(Long.valueOf(2L), captor.getValue().getSubscriptionId());
        assertEquals(0, new BigDecimal("150.00").compareTo(captor.getValue().getAmount()));
        assertEquals(PaymentMethod.CARD, captor.getValue().getMethod());
        assertEquals("TX-1", captor.getValue().getTransactionReference());
    }

    @Test
    void recordPaymentMapsBusinessErrors() throws Exception {
        when(service.recordPayment(any(Requests.PaymentInput.class)))
                .thenThrow(BusinessException.conflict("Référence de transaction déjà utilisée"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Référence de transaction déjà utilisée"));
    }

    @Test
    void recordPaymentRejectsUnknownMethod() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skierId\":1,\"method\":\"CHEQUE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getReturnsPayment() throws Exception {
        when(service.get(5L)).thenReturn(payment(5L, PaymentStatus.PAID));

        mockMvc.perform(get("/payments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getMapsMissingPaymentToNotFound() throws Exception {
        when(service.get(9L)).thenThrow(BusinessException.missing("Paiement"));

        mockMvc.perform(get("/payments/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paiement introuvable"));
    }

    @Test
    void bySkierReturnsPaymentsOfSkier() throws Exception {
        when(service.bySkier(1L)).thenReturn(Arrays.asList(payment(5L, PaymentStatus.PAID), payment(6L, PaymentStatus.PENDING)));

        mockMvc.perform(get("/payments/skier/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].id").value(6));
    }

    @Test
    void bySubscriptionReturnsPaymentsOfSubscription() throws Exception {
        when(service.bySubscription(2L)).thenReturn(Collections.singletonList(payment(5L, PaymentStatus.PAID)));

        mockMvc.perform(get("/payments/subscription/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void statusChangeConvertsPayloadAndDelegates() throws Exception {
        when(service.changeStatus(eq(5L), any(Requests.PaymentState.class))).thenReturn(payment(5L, PaymentStatus.PAID));

        mockMvc.perform(put("/payments/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        ArgumentCaptor<Requests.PaymentState> captor = ArgumentCaptor.forClass(Requests.PaymentState.class);
        verify(service).changeStatus(eq(5L), captor.capture());
        assertEquals(PaymentStatus.PAID, captor.getValue().getStatus());
    }

    @Test
    void statusChangeMapsInvalidTransitionToConflict() throws Exception {
        when(service.changeStatus(eq(5L), any(Requests.PaymentState.class)))
                .thenThrow(BusinessException.conflict("Transition interdite"));

        mockMvc.perform(put("/payments/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REFUNDED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void refundConvertsPayloadAndDelegates() throws Exception {
        when(service.refund(eq(5L), any(Requests.RefundInput.class))).thenReturn(refund(8L));

        mockMvc.perform(post("/payments/5/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.00,\"reason\":\"Erreur\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.reason").value("Erreur"));

        ArgumentCaptor<Requests.RefundInput> captor = ArgumentCaptor.forClass(Requests.RefundInput.class);
        verify(service).refund(eq(5L), captor.capture());
        assertEquals(0, new BigDecimal("20.00").compareTo(captor.getValue().getAmount()));
        assertEquals("Erreur", captor.getValue().getReason());
    }

    @Test
    void refundsReturnsHistory() throws Exception {
        when(service.refunds(5L)).thenReturn(Arrays.asList(refund(8L), refund(9L)));

        mockMvc.perform(get("/payments/5/refunds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    void receiptReturnsReceiptData() throws Exception {
        PaymentReceipt receipt = new PaymentReceipt("R-5", 5L, 1L, "Sahar Saidani", 2L,
                LocalDateTime.of(2030, 1, 1, 10, 0), LocalDateTime.of(2030, 1, 1, 10, 5),
                PaymentMethod.CARD, PaymentStatus.PARTIALLY_REFUNDED, new BigDecimal("150.00"),
                new BigDecimal("20.00"), new BigDecimal("130.00"), Collections.singletonList(refund(8L)));
        when(service.receipt(5L)).thenReturn(receipt);

        mockMvc.perform(get("/payments/5/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("R-5"))
                .andExpect(jsonPath("$.skierName").value("Sahar Saidani"))
                .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.netAmount").value(130.00))
                .andExpect(jsonPath("$.refunds", hasSize(1)));
    }

    @Test
    void receiptMapsMissingPaymentToNotFound() throws Exception {
        when(service.receipt(9L)).thenThrow(BusinessException.missing("Paiement"));

        mockMvc.perform(get("/payments/9/receipt"))
                .andExpect(status().isNotFound());
    }
}

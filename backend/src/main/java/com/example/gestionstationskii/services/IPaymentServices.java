package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Payment;
import com.example.gestionstationskii.entities.Refund;

import java.util.List;

public interface IPaymentServices {
    Payment recordPayment(Requests.PaymentInput input);

    Payment get(Long id);

    Payment changeStatus(Long id, Requests.PaymentState input);

    Refund refund(Long id, Requests.RefundInput input);

    List<Payment> bySkier(Long skierId);

    List<Payment> bySubscription(Long subscriptionId);

    List<Refund> refunds(Long paymentId);

    PaymentReceipt receipt(Long paymentId);
}

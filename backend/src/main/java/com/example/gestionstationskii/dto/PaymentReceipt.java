package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.PaymentMethod;
import com.example.gestionstationskii.entities.PaymentStatus;
import com.example.gestionstationskii.entities.Refund;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Receipt data belongs to the payment aggregate, independently of reporting. */
@Getter
@AllArgsConstructor
public class PaymentReceipt {
    private final String receiptNumber;
    private final Long paymentId;
    private final Long skierId;
    private final String skierName;
    private final Long subscriptionId;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final BigDecimal amount;
    private final BigDecimal refundedAmount;
    private final BigDecimal netAmount;
    private final List<Refund> refunds;
}

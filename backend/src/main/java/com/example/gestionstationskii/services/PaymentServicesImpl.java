package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.*;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServicesImpl implements IPaymentServices {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final ISkierRepository skiers;
    private final ISubscriptionRepository subscriptions;

    @Override
    public Payment recordPayment(Requests.PaymentInput input) {
        Checks.id(input.getSkierId()); Checks.id(input.getSubscriptionId()); Checks.money(input.getAmount());
        Checks.require(input.getAmount().signum() > 0 && input.getMethod() != null, "Montant strictement positif et moyen de paiement requis");
        Skier skier = skiers.lockById(input.getSkierId()).orElseThrow(() -> BusinessException.missing("Skieur"));
        Subscription sub = subscriptions.lockById(input.getSubscriptionId()).orElseThrow(() -> BusinessException.missing("Abonnement"));
        if (skier.getSubscription() == null || !skier.getSubscription().getNumSub().equals(sub.getNumSub()))
            throw BusinessException.conflict("Abonnement non affecté à ce skieur");
        String reference = input.getTransactionReference();
        if (reference != null) {
            Checks.text(reference, "Référence de transaction");
            if (payments.existsByTransactionReference(reference)) throw BusinessException.conflict("Référence de transaction déjà enregistrée");
        }
        Payment p = new Payment(); p.setSkier(skier); p.setSubscription(sub); p.setAmount(input.getAmount());
        p.setMethod(input.getMethod()); p.setPaymentDate(LocalDateTime.now()); p.setStatus(PaymentStatus.PENDING);
        p.setTransactionReference(reference); p.setReceiptNumber("SKI-" + UUID.randomUUID().toString());
        return payments.save(p);
    }
    @Override
    @Transactional(readOnly = true)
    public Payment get(Long id) { Checks.id(id); return payments.findById(id).orElseThrow(() -> BusinessException.missing("Paiement")); }
    private Payment locked(Long id) { Checks.id(id); return payments.lockById(id).orElseThrow(() -> BusinessException.missing("Paiement")); }
    @Override
    public Payment changeStatus(Long id, Requests.PaymentState input) {
        Checks.require(input.getStatus() != null, "Statut requis");
        Payment p = locked(id);
        if (p.getStatus() == input.getStatus()) return p;
        if (p.getStatus() != PaymentStatus.PENDING || (input.getStatus() != PaymentStatus.PAID && input.getStatus() != PaymentStatus.FAILED))
            throw BusinessException.conflict("Transitions autorisées : PENDING vers PAID ou FAILED ; remboursement via /refunds");
        p.setStatus(input.getStatus());
        if (input.getStatus() == PaymentStatus.PAID) p.setPaidAt(LocalDateTime.now());
        return p;
    }
    @Override
    public Refund refund(Long id, Requests.RefundInput input) {
        Checks.money(input.getAmount()); Checks.require(input.getAmount().signum() > 0, "Remboursement strictement positif requis");
        Checks.text(input.getReason(), "Motif");
        Payment p = locked(id);
        if (p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw BusinessException.conflict("Seul un paiement encaissé et non totalement remboursé est remboursable");
        BigDecimal total = p.getRefundedAmount().add(input.getAmount());
        if (total.compareTo(p.getAmount()) > 0) throw BusinessException.conflict("Remboursement supérieur au montant restant");
        Refund r = new Refund(); r.setPayment(p); r.setAmount(input.getAmount()); r.setReason(input.getReason()); r.setRefundDate(LocalDateTime.now());
        p.setRefundedAmount(total); p.setStatus(total.compareTo(p.getAmount()) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        return refunds.save(r);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Payment> bySkier(Long id) { Checks.id(id); if (!skiers.existsById(id)) throw BusinessException.missing("Skieur"); return payments.findBySkierNumSkier(id); }
    @Override
    @Transactional(readOnly = true)
    public List<Payment> bySubscription(Long id) { Checks.id(id); if (!subscriptions.existsById(id)) throw BusinessException.missing("Abonnement"); return payments.findBySubscriptionNumSub(id); }
    @Override
    @Transactional(readOnly = true)
    public List<Refund> refunds(Long id) { get(id); return refunds.findByPaymentId(id); }
    @Override
    @Transactional(readOnly = true)
    public PaymentReceipt receipt(Long id) {
        Payment p = get(id);
        return new PaymentReceipt(p.getReceiptNumber(), p.getId(), p.getSkier().getNumSkier(),
                p.getSkier().getFirstName() + " " + p.getSkier().getLastName(), p.getSubscription().getNumSub(),
                p.getPaymentDate(), p.getPaidAt(), p.getMethod(), p.getStatus(), p.getAmount(), p.getRefundedAmount(),
                p.getPaidAt() == null ? BigDecimal.ZERO : p.getAmount().subtract(p.getRefundedAmount()), refunds.findByPaymentId(id));
    }
}

package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.*;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServicesImpl implements IPaymentServices {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final ISkierRepository skiers;
    private final ISubscriptionRepository subscriptions;

    @Override
    public Payment recordPayment(Requests.PaymentInput input) {
        log.debug("Enregistrement d'un paiement demandé : skieur={}, abonnement={}",
                input.getSkierId(), input.getSubscriptionId());
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
        Payment saved = payments.save(p);
        log.info("Paiement enregistré : id={}, montant={}, statut={}",
                saved.getId(), saved.getAmount(), saved.getStatus());
        return saved;
    }
    @Override
    @Transactional(readOnly = true)
    public Payment get(Long id) {
        Checks.id(id);
        Payment payment = payments.findById(id).orElseThrow(() -> BusinessException.missing("Paiement"));
        log.debug("Paiement récupéré : id={}", id);
        return payment;
    }
    private Payment locked(Long id) { Checks.id(id); return payments.lockById(id).orElseThrow(() -> BusinessException.missing("Paiement")); }
    @Override
    public Payment changeStatus(Long id, Requests.PaymentState input) {
        log.debug("Changement de statut demandé : paiement={}, statut={}", id, input.getStatus());
        Checks.require(input.getStatus() != null, "Statut requis");
        Payment p = locked(id);
        if (p.getStatus() == input.getStatus()) {
            log.debug("Statut inchangé : paiement={}, statut={}", id, p.getStatus());
            return p;
        }
        if (p.getStatus() != PaymentStatus.PENDING || (input.getStatus() != PaymentStatus.PAID && input.getStatus() != PaymentStatus.FAILED))
            throw BusinessException.conflict("Transitions autorisées : PENDING vers PAID ou FAILED ; remboursement via /refunds");
        p.setStatus(input.getStatus());
        if (input.getStatus() == PaymentStatus.PAID) p.setPaidAt(LocalDateTime.now());
        log.info("Statut du paiement modifié : id={}, statut={}", id, p.getStatus());
        return p;
    }
    @Override
    public Refund refund(Long id, Requests.RefundInput input) {
        log.debug("Remboursement demandé : paiement={}, montant={}", id, input.getAmount());
        Checks.money(input.getAmount()); Checks.require(input.getAmount().signum() > 0, "Remboursement strictement positif requis");
        Checks.text(input.getReason(), "Motif");
        Payment p = locked(id);
        if (p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw BusinessException.conflict("Seul un paiement encaissé et non totalement remboursé est remboursable");
        BigDecimal total = p.getRefundedAmount().add(input.getAmount());
        if (total.compareTo(p.getAmount()) > 0) throw BusinessException.conflict("Remboursement supérieur au montant restant");
        Refund r = new Refund(); r.setPayment(p); r.setAmount(input.getAmount()); r.setReason(input.getReason()); r.setRefundDate(LocalDateTime.now());
        p.setRefundedAmount(total); p.setStatus(total.compareTo(p.getAmount()) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        Refund saved = refunds.save(r);
        log.info("Remboursement enregistré : paiement={}, montant={}, statut={}",
                id, saved.getAmount(), p.getStatus());
        return saved;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Payment> bySkier(Long id) {
        Checks.id(id);
        Skier skier = skiers.findById(id).orElseThrow(() -> BusinessException.missing("Skieur"));
        List<Payment> result = payments.findBySkierNumSkier(skier.getNumSkier());
        log.debug("Paiements récupérés par skieur : skieur={}, nombre={}", id, result.size());
        return result;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Payment> bySubscription(Long id) {
        Checks.id(id);
        Subscription subscription = subscriptions.findById(id).orElseThrow(() -> BusinessException.missing("Abonnement"));
        List<Payment> result = payments.findBySubscriptionNumSub(subscription.getNumSub());
        log.debug("Paiements récupérés par abonnement : abonnement={}, nombre={}", id, result.size());
        return result;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Refund> refunds(Long id) {
        get(id);
        List<Refund> result = refunds.findByPaymentId(id);
        log.debug("Remboursements récupérés : paiement={}, nombre={}", id, result.size());
        return result;
    }
    @Override
    @Transactional(readOnly = true)
    public PaymentReceipt receipt(Long id) {
        Payment p = get(id);
        PaymentReceipt receipt = new PaymentReceipt(p.getReceiptNumber(), p.getId(), p.getSkier().getNumSkier(),
                p.getSkier().getFirstName() + " " + p.getSkier().getLastName(), p.getSubscription().getNumSub(),
                p.getPaymentDate(), p.getPaidAt(), p.getMethod(), p.getStatus(), p.getAmount(), p.getRefundedAmount(),
                p.getPaidAt() == null ? BigDecimal.ZERO : p.getAmount().subtract(p.getRefundedAmount()), refunds.findByPaymentId(id));
        log.debug("Reçu de paiement préparé : paiement={}, référence={}", id, p.getReceiptNumber());
        return receipt;
    }
}

package com.example.gestionstationskii.controllers;
import com.example.gestionstationskii.dto.*;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.IPaymentServices;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/payments") @RequiredArgsConstructor @Slf4j
public class PaymentRestController {
    private final IPaymentServices service;
    @Operation(summary = "Enregistrer un paiement local au statut PENDING") @PostMapping
    public Payment recordPayment(@RequestBody Requests.PaymentInput input) {
        log.debug("Requête d'enregistrement de paiement reçue : skieur={}, abonnement={}",
                input.getSkierId(), input.getSubscriptionId());
        Payment payment = service.recordPayment(input);
        log.info("Requête d'enregistrement de paiement terminée : id={}", payment.getId());
        return payment;
    }
    @Operation(summary = "Consulter un paiement") @GetMapping("/{id}")
    public Payment get(@PathVariable Long id) {
        log.debug("Requête de récupération de paiement reçue : id={}", id);
        Payment payment = service.get(id);
        log.debug("Requête de récupération de paiement terminée : id={}", id);
        return payment;
    }
    @Operation(summary = "Paiements d'un skieur") @GetMapping("/skier/{skierId}")
    public List<Payment> bySkier(@PathVariable Long skierId) {
        log.debug("Requête de paiements par skieur reçue : skieur={}", skierId);
        List<Payment> payments = service.bySkier(skierId);
        log.debug("Requête de paiements par skieur terminée : skieur={}, nombre={}",
                skierId, payments.size());
        return payments;
    }
    @Operation(summary = "Paiements d'un abonnement") @GetMapping("/subscription/{subscriptionId}")
    public List<Payment> bySubscription(@PathVariable Long subscriptionId) {
        log.debug("Requête de paiements par abonnement reçue : abonnement={}", subscriptionId);
        List<Payment> payments = service.bySubscription(subscriptionId);
        log.debug("Requête de paiements par abonnement terminée : abonnement={}, nombre={}",
                subscriptionId, payments.size());
        return payments;
    }
    @Operation(summary = "Confirmer ou déclarer l'échec d'un paiement en attente") @PutMapping("/{id}/status")
    public Payment status(@PathVariable Long id, @RequestBody Requests.PaymentState input) {
        log.debug("Requête de changement de statut reçue : paiement={}, statut={}",
                id, input.getStatus());
        Payment payment = service.changeStatus(id, input);
        log.info("Requête de changement de statut terminée : paiement={}, statut={}",
                id, payment.getStatus());
        return payment;
    }
    @Operation(summary = "Enregistrer un remboursement partiel ou total") @PostMapping("/{id}/refunds")
    public Refund refund(@PathVariable Long id, @RequestBody Requests.RefundInput input) {
        log.debug("Requête de remboursement reçue : paiement={}, montant={}",
                id, input.getAmount());
        Refund refund = service.refund(id, input);
        log.info("Requête de remboursement terminée : paiement={}, remboursement={}",
                id, refund.getId());
        return refund;
    }
    @Operation(summary = "Historique des remboursements d'un paiement") @GetMapping("/{id}/refunds")
    public List<Refund> refunds(@PathVariable Long id) {
        log.debug("Requête d'historique des remboursements reçue : paiement={}", id);
        List<Refund> refunds = service.refunds(id);
        log.debug("Requête d'historique des remboursements terminée : paiement={}, nombre={}",
                id, refunds.size());
        return refunds;
    }
    @Operation(summary = "Données du reçu local, avec statut et remboursements") @GetMapping("/{id}/receipt")
    public PaymentReceipt receipt(@PathVariable Long id) {
        log.debug("Requête de reçu reçue : paiement={}", id);
        PaymentReceipt receipt = service.receipt(id);
        log.debug("Requête de reçu terminée : paiement={}", id);
        return receipt;
    }
}

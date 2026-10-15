package com.example.gestionstationskii.controllers;
import com.example.gestionstationskii.dto.*;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.PaymentServicesImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/payments") @RequiredArgsConstructor
public class PaymentRestController {
    private final PaymentServicesImpl service;
    @Operation(summary = "Enregistrer un paiement local au statut PENDING") @PostMapping
    public Payment record(@RequestBody Requests.PaymentInput input) { return service.record(input); }
    @Operation(summary = "Consulter un paiement") @GetMapping("/{id}")
    public Payment get(@PathVariable Long id) { return service.get(id); }
    @Operation(summary = "Paiements d'un skieur") @GetMapping("/skier/{skierId}")
    public List<Payment> bySkier(@PathVariable Long skierId) { return service.bySkier(skierId); }
    @Operation(summary = "Paiements d'un abonnement") @GetMapping("/subscription/{subscriptionId}")
    public List<Payment> bySubscription(@PathVariable Long subscriptionId) { return service.bySubscription(subscriptionId); }
    @Operation(summary = "Confirmer ou déclarer l'échec d'un paiement en attente") @PutMapping("/{id}/status")
    public Payment status(@PathVariable Long id, @RequestBody Requests.PaymentState input) { return service.changeStatus(id, input); }
    @Operation(summary = "Enregistrer un remboursement partiel ou total") @PostMapping("/{id}/refunds")
    public Refund refund(@PathVariable Long id, @RequestBody Requests.RefundInput input) { return service.refund(id, input); }
    @Operation(summary = "Historique des remboursements d'un paiement") @GetMapping("/{id}/refunds")
    public List<Refund> refunds(@PathVariable Long id) { return service.refunds(id); }
    @Operation(summary = "Données du reçu local, avec statut et remboursements") @GetMapping("/{id}/receipt")
    public ReportResponses.Receipt receipt(@PathVariable Long id) { return service.receipt(id); }
}

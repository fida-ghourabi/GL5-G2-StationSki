package com.example.gestionstationskii.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.gestionstationskii.dto.Requests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.example.gestionstationskii.services.*;
import com.example.gestionstationskii.entities.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Tag(name = "\uD83D\uDC65 Subscription Management")
@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRestController {

    private final ISubscriptionServices subscriptionServices;

    @Operation(summary = "Supprimer un abonnement sans skieur ni paiement")
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        log.debug("Requête de suppression d'abonnement reçue : id={}", id);
        subscriptionServices.removeSubscription(id);
        log.info("Requête de suppression d'abonnement terminée : id={}", id);
    }

    @Operation(summary = "Statut calculé de l'abonnement à une date donnée")
    @GetMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate asOf) {
        log.debug("Requête de statut d'abonnement reçue : id={}, date={}", id, asOf);
        String status = subscriptionServices.status(id, asOf);
        log.debug("Réponse de statut d'abonnement : id={}, statut={}", id, status);
        return status;
    }

    @Operation(description = "Add Subscription ")
    @PostMapping("/add")
    public Subscription addSubscription(@RequestBody Requests.SubscriptionInput input){
        Subscription subscription = input.toEntity();
        log.debug("Requête de création d'abonnement reçue : type={}, début={}",
                subscription.getTypeSub(), subscription.getStartDate());
        Subscription saved = subscriptionServices.addSubscription(subscription);
        log.info("Requête de création d'abonnement terminée : id={}", saved.getNumSub());
        return saved;
    }
    @Operation(description = "Retrieve Subscription by Id")
    @GetMapping("/get/{id-subscription}")
    public Subscription getById(@PathVariable("id-subscription") Long numSubscription){
        log.debug("Requête de récupération d'abonnement reçue : id={}", numSubscription);
        Subscription subscription = subscriptionServices.retrieveSubscriptionById(numSubscription);
        log.debug("Requête de récupération d'abonnement terminée : id={}", numSubscription);
        return subscription;
    }

    @Operation(description = "Retrieve Subscriptions by Type")
    @GetMapping("/all/{typeSub}")
    public Set<Subscription> getSubscriptionsByType(@PathVariable("typeSub")TypeSubscription typeSubscription){
        log.debug("Requête de recherche d'abonnements par type reçue : type={}", typeSubscription);
        Set<Subscription> subscriptions = subscriptionServices.getSubscriptionByType(typeSubscription);
        log.debug("Recherche d'abonnements par type terminée : type={}, nombre={}",
                typeSubscription, subscriptions.size());
        return subscriptions;
    }
    @Operation(description = "Update Subscription ")
    @PutMapping("/update")
    public Subscription updateSubscription(@RequestBody Requests.SubscriptionInput input){
        Subscription subscription = input.toEntity();
        log.debug("Requête de mise à jour d'abonnement reçue : id={}", subscription.getNumSub());
        Subscription updated = subscriptionServices.updateSubscription(subscription);
        log.info("Requête de mise à jour d'abonnement terminée : id={}", updated.getNumSub());
        return updated;
    }
    @Operation(description = "Retrieve Subscriptions created between two dates")
    @GetMapping("/all/{date1}/{date2}")
    public List<Subscription> getSubscriptionsByDates(@PathVariable("date1") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                      @PathVariable("date2") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate){
        log.debug("Requête de recherche d'abonnements par période reçue : début={}, fin={}",
                startDate, endDate);
        List<Subscription> subscriptions =
                subscriptionServices.retrieveSubscriptionsByDates(startDate, endDate);
        log.debug("Recherche d'abonnements par période terminée : début={}, fin={}, nombre={}",
                startDate, endDate, subscriptions.size());
        return subscriptions;
    }

}
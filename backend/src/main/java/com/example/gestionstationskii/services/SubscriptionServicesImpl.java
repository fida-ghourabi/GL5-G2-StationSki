package com.example.gestionstationskii.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional
public class SubscriptionServicesImpl implements ISubscriptionServices{

    private ISubscriptionRepository subscriptionRepository;

    private ISkierRepository skierRepository;

    @Override
    public Subscription addSubscription(Subscription subscription) {
        Checks.require(subscription.getNumSub() == null, "Identifiant généré : ne pas le fournir à la création");
        validate(subscription);
        return subscriptionRepository.save(subscription);
    }

    private void validate(Subscription subscription) {
        Checks.require(subscription.getTypeSub() != null && subscription.getStartDate() != null, "Type et date de début requis");
        Checks.price(subscription.getPrice());
        switch (subscription.getTypeSub()) {
            case ANNUAL:
                subscription.setEndDate(subscription.getStartDate().plusYears(1));
                break;
            case SEMESTRIEL:
                subscription.setEndDate(subscription.getStartDate().plusMonths(6));
                break;
            case MONTHLY:
                subscription.setEndDate(subscription.getStartDate().plusMonths(1));
                break;
        }
    }

    @Override
    public Subscription updateSubscription(Subscription subscription) {
        Checks.id(subscription.getNumSub());
        Subscription current = subscriptionRepository.lockById(subscription.getNumSub()).orElseThrow(() -> BusinessException.missing("Abonnement"));
        if (paymentRepository.existsBySubscription_NumSub(current.getNumSub()))
            throw BusinessException.conflict("Abonnement lié à un paiement : historique immuable");
        validate(subscription);
        current.setStartDate(subscription.getStartDate());
        current.setEndDate(subscription.getEndDate());
        current.setTypeSub(subscription.getTypeSub());
        current.setPrice(subscription.getPrice());
        return subscriptionRepository.save(current);
    }

    @Override
    public Subscription retrieveSubscriptionById(Long numSubscription) {
        Checks.id(numSubscription);
        return subscriptionRepository.findById(numSubscription).orElseThrow(() -> BusinessException.missing("Abonnement"));
    }

    @Override
    public Set<Subscription> getSubscriptionByType(TypeSubscription type) {
        Checks.require(type != null, "Type d'abonnement requis");
        return subscriptionRepository.findByTypeSubOrderByStartDateAsc(type);
    }

    @Override
    public List<Subscription> retrieveSubscriptionsByDates(LocalDate startDate, LocalDate endDate) {
        Checks.require(startDate != null && endDate != null && !startDate.isAfter(endDate), "Période invalide");
        return subscriptionRepository.getSubscriptionsByStartDateBetween(startDate, endDate);
    }

    @Override
    public void retrieveSubscriptions() {
        for (Subscription sub: subscriptionRepository.findDistinctOrderByEndDateAsc()) {
            Skier   aSkier = skierRepository.findBySubscription(sub);
            if (aSkier == null) continue;
            log.info(sub.getNumSub().toString() + " | "+ sub.getEndDate().toString()
                    + " | "+ aSkier.getFirstName() + " " + aSkier.getLastName());
        }
    }

    /** Theoretical recurring revenue, not cash collected. Deliberately not scheduled. */
    public void showMonthlyRecurringRevenue() {
        Float revenue = subscriptionRepository.recurringRevenueByTypeSubEquals(TypeSubscription.MONTHLY)
                + subscriptionRepository.recurringRevenueByTypeSubEquals(TypeSubscription.SEMESTRIEL)/6
                + subscriptionRepository.recurringRevenueByTypeSubEquals(TypeSubscription.ANNUAL)/12;
        log.info("Monthly Revenue = " + revenue);
    }

    private PaymentRepository paymentRepository;

    public void removeSubscription(Long id) {
        Checks.id(id);
        Subscription sub = subscriptionRepository.lockById(id).orElseThrow(() -> BusinessException.missing("Abonnement"));
        if (paymentRepository.existsBySubscription_NumSub(id) || skierRepository.findBySubscription(sub) != null)
            throw BusinessException.conflict("Abonnement affecté ou lié à un paiement : suppression refusée");
        subscriptionRepository.delete(sub);
    }

    public String status(Long id, LocalDate asOf) {
        Subscription sub = retrieveSubscriptionById(id);
        Checks.require(asOf != null, "Date de référence requise");
        if (sub.getStartDate() == null || sub.getEndDate() == null) throw BusinessException.conflict("Abonnement historique sans dates : corriger les données");
        return asOf.isBefore(sub.getStartDate()) ? "UPCOMING" : (!asOf.isBefore(sub.getEndDate()) ? "EXPIRED" : "ACTIVE");
    }
}

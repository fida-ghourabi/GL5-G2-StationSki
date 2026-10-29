package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SubscriptionServicesImpl implements ISubscriptionServices {

    private static final String SUBSCRIPTION = "Abonnement";

    private final ISubscriptionRepository subscriptionRepository;

    private final ISkierRepository skierRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public Subscription addSubscription(Subscription subscription) {
        log.debug("Création d'un abonnement demandée");
        Checks.require(subscription.getNumSub() == null, "Identifiant généré : ne pas le fournir à la création");
        validate(subscription);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Abonnement créé : id={}, type={}, début={}, fin={}",
                saved.getNumSub(), saved.getTypeSub(), saved.getStartDate(), saved.getEndDate());
        return saved;
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
        log.debug("Abonnement validé : type={}, début={}, fin={}",
                subscription.getTypeSub(), subscription.getStartDate(), subscription.getEndDate());
    }

    @Override
    public Subscription updateSubscription(Subscription subscription) {
        log.debug("Mise à jour de l'abonnement demandée : id={}", subscription.getNumSub());
        Checks.id(subscription.getNumSub());
        Subscription current = subscriptionRepository.lockById(subscription.getNumSub()).orElseThrow(() -> BusinessException.missing(SUBSCRIPTION));
        if (paymentRepository.existsBySubscription_NumSub(current.getNumSub())) {
            log.warn("Mise à jour refusée : abonnement lié à un paiement, id={}", current.getNumSub());
            throw BusinessException.conflict("Abonnement lié à un paiement : historique immuable");
        }
        validate(subscription);
        current.setStartDate(subscription.getStartDate());
        current.setEndDate(subscription.getEndDate());
        current.setTypeSub(subscription.getTypeSub());
        current.setPrice(subscription.getPrice());
        Subscription updated = subscriptionRepository.save(current);
        log.info("Abonnement mis à jour : id={}, type={}, début={}, fin={}",
                updated.getNumSub(), updated.getTypeSub(), updated.getStartDate(), updated.getEndDate());
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription retrieveSubscriptionById(Long numSubscription) {
        return findSubscription(numSubscription);
    }

    private Subscription findSubscription(Long numSubscription) {
        Checks.id(numSubscription);
        Subscription subscription = subscriptionRepository.findById(numSubscription)
                .orElseThrow(() -> BusinessException.missing(SUBSCRIPTION));
        log.debug("Abonnement récupéré : id={}", numSubscription);
        return subscription;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Subscription> getSubscriptionByType(TypeSubscription type) {
        Checks.require(type != null, "Type d'abonnement requis");
        Set<Subscription> subscriptions = subscriptionRepository.findByTypeSubOrderByStartDateAsc(type);
        log.debug("Abonnements récupérés par type : type={}, nombre={}", type, subscriptions.size());
        return subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> retrieveSubscriptionsByDates(LocalDate startDate, LocalDate endDate) {
        Checks.require(startDate != null && endDate != null && !startDate.isAfter(endDate), "Période invalide");
        List<Subscription> subscriptions =
                subscriptionRepository.getSubscriptionsByStartDateBetween(startDate, endDate);
        log.debug("Abonnements récupérés par période : début={}, fin={}, nombre={}",
                startDate, endDate, subscriptions.size());
        return subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public void retrieveSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findDistinctOrderByEndDateAsc();
        log.debug("Abonnements expirés récupérés : nombre={}", subscriptions.size());
        for (Subscription sub: subscriptions) {
            Skier   aSkier = skierRepository.findBySubscription(sub);
            if (aSkier == null) continue;
            log.info("Abonnement expiré associé à un skieur : id={}, fin={}, skieur={}",
                    sub.getNumSub(), sub.getEndDate(),
                    aSkier.getFirstName() + " " + aSkier.getLastName());
        }
    }

    @Override
    public void removeSubscription(Long id) {
        log.debug("Suppression de l'abonnement demandée : id={}", id);
        Checks.id(id);
        Subscription sub = subscriptionRepository.lockById(id).orElseThrow(() -> BusinessException.missing(SUBSCRIPTION));
        if (paymentRepository.existsBySubscription_NumSub(id) || skierRepository.findBySubscription(sub) != null) {
            log.warn("Suppression refusée : abonnement affecté ou lié à un paiement, id={}", id);
            throw BusinessException.conflict("Abonnement affecté ou lié à un paiement : suppression refusée");
        }
        subscriptionRepository.delete(sub);
        log.info("Abonnement supprimé : id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public String status(Long id, LocalDate asOf) {
        Subscription sub = findSubscription(id);
        Checks.require(asOf != null, "Date de référence requise");
        if (sub.getStartDate() == null || sub.getEndDate() == null) throw BusinessException.conflict("Abonnement historique sans dates : corriger les données");
        String status;
        if (asOf.isBefore(sub.getStartDate())) {
            status = "UPCOMING";
        } else if (!asOf.isBefore(sub.getEndDate())) {
            status = "EXPIRED";
        } else {
            status = "ACTIVE";
        }
        log.debug("Statut de l'abonnement calculé : id={}, date={}, statut={}", id, asOf, status);
        return status;
    }
}

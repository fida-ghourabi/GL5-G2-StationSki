package com.example.gestionstationskii.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class SkierServicesImpl implements ISkierServices {

    private ISkierRepository skierRepository;

    private IPisteRepository pisteRepository;

    private ICourseRepository courseRepository;

    private IRegistrationRepository registrationRepository;

    private ISubscriptionRepository subscriptionRepository;
    private SubscriptionServicesImpl subscriptionServices;
    private RegistrationServicesImpl registrationServices;
    private RentalRepository rentalRepository;
    private PaymentRepository paymentRepository;


    @Override
    public List<Skier> retrieveAllSkiers() {
        return skierRepository.findAll();
    }

    @Override
    public Skier addSkier(Skier skier) {
        Checks.require(skier.getNumSkier() == null, "Identifiant généré à la création");
        validate(skier);
        if (skier.getSubscription() != null) {
            Subscription sub = skier.getSubscription();
            if (sub.getNumSub() == null) skier.setSubscription(subscriptionServices.addSubscription(sub));
            else {
                sub = subscriptionServices.retrieveSubscriptionById(sub.getNumSub());
                if (skierRepository.findBySubscription(sub) != null) throw BusinessException.conflict("Abonnement déjà affecté");
                skier.setSubscription(sub);
            }
        }
        skier.setPistes(new HashSet<>());
        skier.setRegistrations(new HashSet<>());
        return skierRepository.save(skier);
    }

    @Override
    public Skier assignSkierToSubscription(Long numSkier, Long numSubscription) {
        Checks.id(numSkier); Checks.id(numSubscription);
        Skier skier = skierRepository.lockById(numSkier).orElseThrow(() -> BusinessException.missing("Skieur"));
        Subscription subscription = subscriptionRepository.lockById(numSubscription).orElseThrow(() -> BusinessException.missing("Abonnement"));
        Skier owner = skierRepository.findBySubscription(subscription);
        if (owner != null) throw BusinessException.conflict("Abonnement déjà affecté");
        skier.setSubscription(subscription);
        return skierRepository.save(skier);
    }

    @Override
    public Skier addSkierAndAssignToCourse(Skier skier, Long numCourse) {
        Checks.id(numCourse);
        courseRepository.findById(numCourse).orElseThrow(() -> BusinessException.missing("Cours"));
        Skier savedSkier = addSkier(skier);
        PlanningSlot slot = registrationServices.nextAvailableSlot(numCourse);
        registrationServices.registerSlot(savedSkier.getNumSkier(), slot.getId());
        return savedSkier;
    }

    @Override
    public void removeSkier(Long numSkier) {
        Skier skier = retrieveSkier(numSkier);
        if (registrationRepository.existsBySkier_NumSkier(numSkier) || rentalRepository.existsBySkier_NumSkier(numSkier)
                || paymentRepository.existsBySkier_NumSkier(numSkier))
            throw BusinessException.conflict("Skieur ayant un historique d'inscription, location ou paiement : suppression refusée");
        skierRepository.delete(skier);
    }

    @Override
    public Skier retrieveSkier(Long numSkier) {
        Checks.id(numSkier);
        return skierRepository.findById(numSkier).orElseThrow(() -> BusinessException.missing("Skieur"));
    }

    @Override
    public Skier assignSkierToPiste(Long numSkieur, Long numPiste) {
        Checks.id(numSkieur); Checks.id(numPiste);
        Skier skier = skierRepository.lockById(numSkieur).orElseThrow(() -> BusinessException.missing("Skieur"));
        Piste piste = pisteRepository.findById(numPiste).orElseThrow(() -> BusinessException.missing("Piste"));
        if (skier.getPistes() == null) skier.setPistes(new HashSet<>());
        if (!skier.getPistes().add(piste)) throw BusinessException.conflict("Skieur déjà affecté à cette piste");

        return skierRepository.save(skier);
    }

    @Override
    public List<Skier> retrieveSkiersBySubscriptionType(TypeSubscription typeSubscription) {
        Checks.require(typeSubscription != null, "Type d'abonnement requis");
        return skierRepository.findBySubscription_TypeSub(typeSubscription);
    }

    private void validate(Skier skier) {
        Checks.text(skier.getFirstName(), "Prénom"); Checks.text(skier.getLastName(), "Nom"); Checks.text(skier.getCity(), "Ville");
        Checks.require(skier.getDateOfBirth() != null && skier.getDateOfBirth().isBefore(java.time.LocalDate.now()), "Date de naissance passée requise");
    }
    public Skier updateSkier(Skier input) {
        validate(input);
        Checks.id(input.getNumSkier());
        Skier current = skierRepository.lockById(input.getNumSkier()).orElseThrow(() -> BusinessException.missing("Skieur"));
        current.setFirstName(input.getFirstName()); current.setLastName(input.getLastName());
        current.setCity(input.getCity()); current.setDateOfBirth(input.getDateOfBirth());
        return skierRepository.save(current);
    }
    public org.springframework.data.domain.Page<Skier> search(String name, String city, int page, int size) {
        Checks.require(page >= 0 && size > 0 && size <= 100, "Pagination invalide (taille 1 à 100)");
        return skierRepository.search(name, city, org.springframework.data.domain.PageRequest.of(page, size));
    }
}

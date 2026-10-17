package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.util.HashSet;
import java.util.List;

@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class SkierServicesImpl implements ISkierServices {

    private final ISkierRepository skierRepository;

    private final IPisteRepository pisteRepository;

    private final ICourseRepository courseRepository;

    private final IRegistrationRepository registrationRepository;

    private final ISubscriptionRepository subscriptionRepository;
    private final ISubscriptionServices subscriptionServices;
    private final IRegistrationServices registrationServices;
    private final PaymentRepository paymentRepository;


    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Skier> retrieveAllSkiers() {
        return logged("list", null, () -> {
            List<Skier> skiers = skierRepository.findAll();
            log.debug("Skier list returned count={}", skiers.size());
            return skiers;
        });
    }

    @Override
    public Skier addSkier(Skier skier) {
        return logged("create", null, () -> {
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
        Skier saved = skierRepository.save(skier);
        log.info("Skier created id={}", saved.getNumSkier());
        return saved;
        });
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
        logged("delete", numSkier, () -> {
        Skier skier = retrieveSkier(numSkier);
        if (registrationRepository.existsBySkier_NumSkier(numSkier)
                || paymentRepository.existsBySkier_NumSkier(numSkier))
            throw BusinessException.conflict("Skieur ayant un historique d'inscription ou de paiement : suppression refusée");
        skierRepository.delete(skier);
        log.info("Skier deletion submitted id={}", numSkier);
        return null;
        });
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Skier retrieveSkier(Long numSkier) {
        return logged("read", numSkier, () -> {
            Checks.id(numSkier);
            return skierRepository.findById(numSkier).orElseThrow(() -> BusinessException.missing("Skieur"));
        });
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
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Skier> retrieveSkiersBySubscriptionType(TypeSubscription typeSubscription) {
        Checks.require(typeSubscription != null, "Type d'abonnement requis");
        return skierRepository.findBySubscription_TypeSub(typeSubscription);
    }

    // Do not log names, birth dates, cities or full entity objects.
    // Completion here is inside the transaction, before its eventual commit.
    private <T> T logged(String operation, Long id, java.util.function.Supplier<T> action) {
        log.debug("Skier operation={} requested id={}", operation, id);
        try {
            T result = action.get();
            log.debug("Skier operation={} completed id={}", operation, id);
            return result;
        } catch (RuntimeException ex) {
            log.error("Skier operation={} failed id={}", operation, id, ex);
            throw ex;
        }
    }

    private void validate(Skier skier) {
        Checks.text(skier.getFirstName(), "Prénom"); Checks.text(skier.getLastName(), "Nom"); Checks.text(skier.getCity(), "Ville");
        Checks.require(skier.getDateOfBirth() != null && skier.getDateOfBirth().isBefore(java.time.LocalDate.now()), "Date de naissance passée requise");
    }
    @Override
    public Skier updateSkier(Skier input) {
        return logged("update", input == null ? null : input.getNumSkier(), () -> {
        validate(input);
        Checks.id(input.getNumSkier());
        Skier current = skierRepository.lockById(input.getNumSkier()).orElseThrow(() -> BusinessException.missing("Skieur"));
        current.setFirstName(input.getFirstName()); current.setLastName(input.getLastName());
        boolean profileChanged = !java.util.Objects.equals(current.getCity(), input.getCity());
        current.setCity(input.getCity()); current.setDateOfBirth(input.getDateOfBirth());
        Skier saved = skierRepository.save(current);
        log.info("Skier updated id={} cityChanged={}", saved.getNumSkier(), profileChanged);
        return saved;
        });
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public org.springframework.data.domain.Page<Skier> search(String name, String city, int page, int size) {
        Checks.require(page >= 0 && size > 0 && size <= 100, "Pagination invalide (taille 1 à 100)");
        return skierRepository.search(name, city, org.springframework.data.domain.PageRequest.of(page, size));
    }
}

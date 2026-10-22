package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import javax.persistence.EntityManager;
import java.time.Period;
import java.util.List;
@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RegistrationServicesImpl implements IRegistrationServices {

    private final IRegistrationRepository registrationRepository;
    private final ISkierRepository skierRepository;
    private final ICourseRepository courseRepository;
    private final PlanningSlotRepository slotRepository;
    private final IInstructorRepository instructorRepository;
    private final EntityManager entityManager;


    @Override
    public Registration addRegistrationAndAssignToSkier(Registration registration, Long numSkier) {
        Checks.require(registration.getNumRegistration() == null, "Identifiant généré à la création");
        Checks.require(registration.getPlanningSlot() != null, "Créneau requis ; utiliser /registration/skier/{id}/slot/{id}");
        return registerSlot(numSkier, registration.getPlanningSlot().getId());
    }

    @Override
    public Registration assignRegistrationToCourse(Long numRegistration, Long numCourse) {
        Checks.id(numRegistration); Checks.id(numCourse);
        Registration registration = registrationRepository.findById(numRegistration).orElseThrow(() -> BusinessException.missing("Inscription"));
        courseRepository.findById(numCourse).orElseThrow(() -> BusinessException.missing("Cours"));
        if (registration.isCancelled()) throw BusinessException.conflict("Inscription annulée");
        if (registration.getCourse() != null && registration.getCourse().getNumCourse().equals(numCourse)) return registration;
        throw BusinessException.conflict("Une inscription ne change pas de cours ; annuler puis réinscrire sur un créneau");
    }

    @Override
    public Registration addRegistrationAndAssignToSkierAndCourse(Registration registration, Long numSkieur, Long numCours) {
        Checks.id(numCours);
        courseRepository.findById(numCours).orElseThrow(() -> BusinessException.missing("Cours"));
        Checks.require(registration.getNumRegistration() == null, "Identifiant généré à la création");
        Checks.require(registration.getPlanningSlot() != null, "Créneau requis pour toute nouvelle inscription");
        Checks.id(registration.getPlanningSlot().getId());
        PlanningSlot slot = slotRepository.findById(registration.getPlanningSlot().getId()).orElseThrow(() -> BusinessException.missing("Créneau"));
        if (!slot.getCourse().getNumCourse().equals(numCours)) throw BusinessException.conflict("Créneau associé à un autre cours");
        return registerSlot(numSkieur, slot.getId());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Integer> numWeeksCourseOfInstructorBySupport(Long numInstructor, Support support) {
        Checks.id(numInstructor); Checks.require(support != null, "Support requis");
        instructorRepository.findById(numInstructor).orElseThrow(() -> BusinessException.missing("Moniteur"));
        return registrationRepository.numWeeksCourseOfInstructorBySupport(numInstructor, support);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public PlanningSlot nextAvailableSlot(Long courseId) {
        return slotRepository.findAll().stream().filter(s -> s.getCourse().getNumCourse().equals(courseId)
                && s.getStatus() == SlotStatus.OPEN && s.getStartTime().isAfter(java.time.LocalDateTime.now()))
                .min(java.util.Comparator.comparing(PlanningSlot::getStartTime))
                .orElseThrow(() -> BusinessException.conflict("Aucun créneau disponible pour ce cours"));
    }

    @Override
    public Registration registerSlot(Long skierId, Long slotId) {
        log.debug("Processing registration request for slot {}", slotId);
        Checks.id(skierId); Checks.id(slotId);
        // Serialize per skier (schedule conflicts), then per slot (capacity).
        Skier skier = skierRepository.lockById(skierId).orElseThrow(() -> BusinessException.missing("Skieur"));
        PlanningSlot slot = slotRepository.lockById(slotId).orElseThrow(() -> BusinessException.missing("Créneau"));
        entityManager.refresh(slot);
        if (slot.getStatus() != SlotStatus.OPEN || !slot.getStartTime().isAfter(java.time.LocalDateTime.now())) {
            log.warn("Registration rejected: slot {} is closed or has already started", slotId);
            throw BusinessException.conflict("Créneau complet, annulé ou déjà commencé");
        }
        Course course = slot.getCourse();
        Checks.require(skier.getDateOfBirth() != null, "Date de naissance du skieur requise");
        int age = Period.between(skier.getDateOfBirth(), slot.getStartTime().toLocalDate()).getYears();
        if ((course.getTypeCourse() == TypeCourse.COLLECTIVE_CHILDREN && age >= 16)
                || (course.getTypeCourse() == TypeCourse.COLLECTIVE_ADULT && age < 16)) {
            log.warn("Registration rejected: age rule does not match course type for slot {}", slotId);
            throw BusinessException.conflict("Âge incompatible avec le cours (seuil : 16 ans)");
        }
        for (Registration r : registrationRepository.findBySkier_NumSkier(skierId)) {
            if (r.isCancelled()) continue;
            PlanningSlot other = r.getPlanningSlot();
            if (other != null && other.getStatus() != SlotStatus.CANCELLED
                    && TimeRanges.overlaps(other.getStartTime(), other.getEndTime(), slot.getStartTime(), slot.getEndTime()))
                throw BusinessException.conflict("Inscription en double ou conflit horaire du skieur");
            if (other != null && other.getStatus() != SlotStatus.CANCELLED && r.getCourse().getNumCourse().equals(course.getNumCourse())
                    && other.getStartTime().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR) == slot.getStartTime().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                    && other.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR) == slot.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                throw BusinessException.conflict("Skieur déjà inscrit à ce cours pendant cette semaine ISO");
            if (other == null && r.getCourse() != null && r.getCourse().getNumCourse().equals(course.getNumCourse())
                    && r.getNumWeek() == slot.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                throw BusinessException.conflict("Inscription historique déjà présente cette semaine (année inconnue)");
        }
        long count = registrationRepository.findByPlanningSlot_Id(slotId).stream().filter(r -> !r.isCancelled()).count();
        if (count >= slot.getMaxCapacity()) {
            log.warn("Registration rejected: slot {} has reached capacity", slotId);
            throw BusinessException.conflict("Capacité du créneau atteinte");
        }
        Registration r = new Registration();
        r.setSkier(skier); r.setCourse(course); r.setPlanningSlot(slot);
        r.setNumWeek(slot.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        if (count + 1 >= slot.getMaxCapacity()) slot.setStatus(SlotStatus.FULL);
        Registration saved;
        try {
            saved = registrationRepository.save(r);
        } catch (RuntimeException exception) {
            log.error("Failed to create registration for slot {}", slotId, exception);
            throw exception;
        }
        log.info("Registration {} created for slot {}", saved.getNumRegistration(), slotId);
        return saved;
    }
    @Override
    public void cancel(Long id) {
        Checks.id(id);
        Registration initial = registrationRepository.findById(id).orElseThrow(() -> BusinessException.missing("Inscription"));
        skierRepository.lockById(initial.getSkier().getNumSkier()).orElseThrow(() -> BusinessException.missing("Skieur"));
        PlanningSlot slot = initial.getPlanningSlot() == null ? null : slotRepository.lockById(initial.getPlanningSlot().getId()).orElseThrow(() -> BusinessException.missing("Créneau"));
        entityManager.refresh(initial);
        if (slot != null) entityManager.refresh(slot);
        if (initial.isCancelled()) {
            log.debug("Registration {} was already cancelled", id);
            return;
        }
        if (slot != null && !slot.getStartTime().isAfter(java.time.LocalDateTime.now())) {
            log.warn("Cancellation rejected: registration {} has already started", id);
            throw BusinessException.conflict("Inscription commencée non annulable");
        }
        initial.setCancelled(true);
        if (slot != null && slot.getStatus() == SlotStatus.FULL) slot.setStatus(SlotStatus.OPEN);
        log.info("Registration {} cancelled", id);
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Registration> bySkier(Long id) {
        Checks.id(id); skierRepository.findById(id).orElseThrow(() -> BusinessException.missing("Skieur"));
        return registrationRepository.findBySkier_NumSkier(id);
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Registration> search(java.time.LocalDateTime from, java.time.LocalDateTime to, Long courseId) {
        Checks.require(from != null && to != null && from.isBefore(to), "Période invalide");
        if (courseId != null) { Checks.id(courseId); courseRepository.findById(courseId).orElseThrow(() -> BusinessException.missing("Cours")); }
        return registrationRepository.findAll().stream().filter(r -> r.getPlanningSlot() != null
                && (courseId == null || r.getCourse().getNumCourse().equals(courseId))
                && TimeRanges.overlaps(r.getPlanningSlot().getStartTime(), r.getPlanningSlot().getEndTime(), from, to))
                .collect(java.util.stream.Collectors.toList());
    }

}

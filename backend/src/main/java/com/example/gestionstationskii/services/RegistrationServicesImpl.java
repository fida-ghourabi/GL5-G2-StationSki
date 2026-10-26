package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import javax.persistence.EntityManager;
import java.time.Period;
import java.util.List;
@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RegistrationServicesImpl implements IRegistrationServices {
    private static final String COURSE_RESOURCE = "Cours";
    private static final String SLOT_RESOURCE = "Créneau";
    private static final String SKIER_RESOURCE = "Skieur";
    private static final String INSTRUCTOR_RESOURCE = "Moniteur";

    private final IRegistrationRepository registrationRepository;
    private final ISkierRepository skierRepository;
    private final ICourseRepository courseRepository;
    private final PlanningSlotRepository slotRepository;
    private final IInstructorRepository instructorRepository;
    private final EntityManager entityManager;


    @Override
    public Registration addRegistrationAndAssignToSkier(Registration registration, Long numSkier) {
        log.debug("Adding registration for skier {}", numSkier);
        Checks.require(registration.getNumRegistration() == null, "Identifiant généré à la création");
        Checks.require(registration.getPlanningSlot() != null, "Créneau requis ; utiliser /registration/skier/{id}/slot/{id}");
        return registerSlot(numSkier, registration.getPlanningSlot().getId());
    }

    @Override
    public Registration assignRegistrationToCourse(Long numRegistration, Long numCourse) {
        log.debug("Assigning registration {} to course {}", numRegistration, numCourse);
        Checks.id(numRegistration); Checks.id(numCourse);
        Registration registration = registrationRepository.findById(numRegistration).orElseThrow(() -> BusinessException.missing("Inscription"));
        if (!courseRepository.existsById(numCourse)) throw BusinessException.missing(COURSE_RESOURCE);
        if (registration.isCancelled()) throw BusinessException.conflict("Inscription annulée");
        if (registration.getCourse() != null && registration.getCourse().getNumCourse().equals(numCourse)) return registration;
        throw BusinessException.conflict("Une inscription ne change pas de cours ; annuler puis réinscrire sur un créneau");
    }

    @Override
    public Registration addRegistrationAndAssignToSkierAndCourse(Registration registration, Long numSkieur, Long numCours) {
        log.debug("Adding registration for skier {} and course {}", numSkieur, numCours);
        Checks.id(numCours);
        if (!courseRepository.existsById(numCours)) throw BusinessException.missing(COURSE_RESOURCE);
        Checks.require(registration.getNumRegistration() == null, "Identifiant généré à la création");
        Checks.require(registration.getPlanningSlot() != null, "Créneau requis pour toute nouvelle inscription");
        Checks.id(registration.getPlanningSlot().getId());
        PlanningSlot slot = slotRepository.findById(registration.getPlanningSlot().getId()).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
        if (!slot.getCourse().getNumCourse().equals(numCours)) throw BusinessException.conflict("Créneau associé à un autre cours");
        return registerSlot(numSkieur, slot.getId());
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Integer> numWeeksCourseOfInstructorBySupport(Long numInstructor, Support support) {
        log.debug("Finding course weeks for instructor {} and support {}", numInstructor, support);
        Checks.id(numInstructor); Checks.require(support != null, "Support requis");
        if (!instructorRepository.existsById(numInstructor)) throw BusinessException.missing(INSTRUCTOR_RESOURCE);
        return registrationRepository.numWeeksCourseOfInstructorBySupport(numInstructor, support);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public PlanningSlot nextAvailableSlot(Long courseId) {
        log.debug("Finding next available slot for course {}", courseId);
        return slotRepository.findAll().stream().filter(s -> s.getCourse().getNumCourse().equals(courseId)
                && s.getStatus() == SlotStatus.OPEN && s.getStartTime().isAfter(java.time.LocalDateTime.now()))
                .min(java.util.Comparator.comparing(PlanningSlot::getStartTime))
                .orElseThrow(() -> BusinessException.conflict("Aucun créneau disponible pour ce cours"));
    }

    @Override
    public Registration registerSlot(Long skierId, Long slotId) {
        log.debug("Processing registration request: skier {} on slot {}", skierId, slotId);
        Checks.id(skierId); Checks.id(slotId);
        // Serialize per skier (schedule conflicts), then per slot (capacity).
        Skier skier = skierRepository.lockById(skierId).orElseThrow(() -> BusinessException.missing(SKIER_RESOURCE));
        PlanningSlot slot = slotRepository.lockById(slotId).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
        entityManager.refresh(slot);
        ensureSlotCanAcceptRegistration(slot, slotId);
        Course course = slot.getCourse();
        Checks.require(skier.getDateOfBirth() != null, "Date de naissance du skieur requise");
        ensureAgeMatchesCourse(skier, course, slot, slotId);
        ensureNoConflictingRegistration(skierId, course, slot);
        long count = registrationRepository.findByPlanningSlotId(slotId).stream().filter(r -> !r.isCancelled()).count();
        ensureSlotHasCapacity(slot, slotId, count);
        Registration r = new Registration();
        r.setSkier(skier); r.setCourse(course); r.setPlanningSlot(slot);
        r.setNumWeek(slot.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        if (count + 1 >= slot.getMaxCapacity()) slot.setStatus(SlotStatus.FULL);
        Registration saved;
        try {
            saved = registrationRepository.save(r);
        } catch (DataIntegrityViolationException | ConcurrencyFailureException ex) {
            log.error("Technical failure while saving registration for skier {} on slot {}", skierId, slotId, ex);
            throw ex;
        }
        log.info("Registration {} created for slot {}", saved.getNumRegistration(), slotId);
        return saved;
    }

    private void ensureSlotCanAcceptRegistration(PlanningSlot slot, Long slotId) {
        if (slot.getStatus() != SlotStatus.OPEN || !slot.getStartTime().isAfter(java.time.LocalDateTime.now())) {
            log.warn("Registration rejected: slot {} is closed or has already started", slotId);
            throw BusinessException.conflict("Créneau complet, annulé ou déjà commencé");
        }
    }

    private void ensureAgeMatchesCourse(Skier skier, Course course, PlanningSlot slot, Long slotId) {
        int age = Period.between(skier.getDateOfBirth(), slot.getStartTime().toLocalDate()).getYears();
        boolean childInAdultCourse = course.getTypeCourse() == TypeCourse.COLLECTIVE_CHILDREN && age >= 16;
        boolean adultInChildrenCourse = course.getTypeCourse() == TypeCourse.COLLECTIVE_ADULT && age < 16;
        if (childInAdultCourse || adultInChildrenCourse) {
            log.warn("Registration rejected: age rule does not match course type for slot {}", slotId);
            throw BusinessException.conflict("Âge incompatible avec le cours (seuil : 16 ans)");
        }
    }

    private void ensureNoConflictingRegistration(Long skierId, Course course, PlanningSlot slot) {
        for (Registration r : registrationRepository.findBySkierNumSkier(skierId)) {
            if (r.isCancelled()) continue;
            PlanningSlot other = r.getPlanningSlot();
            if (hasScheduleOverlap(other, slot))
                throw BusinessException.conflict("Inscription en double ou conflit horaire du skieur");
            if (sameCourseInSameIsoWeek(r, other, course, slot))
                throw BusinessException.conflict("Skieur déjà inscrit à ce cours pendant cette semaine ISO");
            if (sameLegacyCourseWeek(r, other, course, slot))
                throw BusinessException.conflict("Inscription historique déjà présente cette semaine (année inconnue)");
        }
    }

    private boolean hasScheduleOverlap(PlanningSlot other, PlanningSlot requested) {
        return other != null && other.getStatus() != SlotStatus.CANCELLED
                && TimeRanges.overlaps(other.getStartTime(), other.getEndTime(), requested.getStartTime(), requested.getEndTime());
    }

    private boolean sameCourseInSameIsoWeek(Registration registration, PlanningSlot other, Course course, PlanningSlot requested) {
        return other != null && other.getStatus() != SlotStatus.CANCELLED
                && registration.getCourse().getNumCourse().equals(course.getNumCourse())
                && other.getStartTime().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                == requested.getStartTime().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                && other.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                == requested.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private boolean sameLegacyCourseWeek(Registration registration, PlanningSlot other, Course course, PlanningSlot requested) {
        return other == null && registration.getCourse() != null
                && registration.getCourse().getNumCourse().equals(course.getNumCourse())
                && registration.getNumWeek() == requested.getStartTime().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private void ensureSlotHasCapacity(PlanningSlot slot, Long slotId, long count) {
        if (count >= slot.getMaxCapacity()) {
            log.warn("Registration rejected: slot {} has reached capacity", slotId);
            throw BusinessException.conflict("Capacité du créneau atteinte");
        }
    }
    @Override
    public void cancel(Long id) {
        Checks.id(id);
        Registration initial = registrationRepository.findById(id).orElseThrow(() -> BusinessException.missing("Inscription"));
        skierRepository.lockById(initial.getSkier().getNumSkier()).orElseThrow(() -> BusinessException.missing(SKIER_RESOURCE));
        PlanningSlot slot = initial.getPlanningSlot() == null ? null : slotRepository.lockById(initial.getPlanningSlot().getId()).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
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
        log.debug("Finding registrations for skier {}", id);
        Checks.id(id); if (!skierRepository.existsById(id)) throw BusinessException.missing(SKIER_RESOURCE);
        return registrationRepository.findBySkierNumSkier(id);
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Registration> search(java.time.LocalDateTime from, java.time.LocalDateTime to, Long courseId) {
        log.debug("Searching registrations from {} to {} for course {}", from, to, courseId);
        Checks.require(from != null && to != null && from.isBefore(to), "Période invalide");
        if (courseId != null) { Checks.id(courseId); if (!courseRepository.existsById(courseId)) throw BusinessException.missing(COURSE_RESOURCE); }
        return registrationRepository.findAll().stream().filter(r -> r.getPlanningSlot() != null
                && (courseId == null || r.getCourse().getNumCourse().equals(courseId))
                && TimeRanges.overlaps(r.getPlanningSlot().getStartTime(), r.getPlanningSlot().getEndTime(), from, to))
                .collect(java.util.stream.Collectors.toList());
    }

}

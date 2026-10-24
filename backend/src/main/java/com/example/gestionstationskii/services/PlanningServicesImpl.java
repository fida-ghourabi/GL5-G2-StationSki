package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PlanningServicesImpl implements IPlanningServices {
    private static final String INSTRUCTOR_RESOURCE = "Moniteur";
    private static final String SLOT_RESOURCE = "Créneau";
    private static final String COURSE_RESOURCE = "Cours";
    private final PlanningSlotRepository slots;
    private final InstructorAvailabilityRepository availabilities;
    private final IInstructorRepository instructors;
    private final ICourseRepository courses;
    private final IRegistrationRepository registrations;

    @Override
    public InstructorAvailability addAvailability(Long instructorId, Requests.Availability input) {
        Checks.id(instructorId);
        Instructor instructor = instructors.lockById(instructorId).orElseThrow(() -> BusinessException.missing(INSTRUCTOR_RESOURCE));
        Checks.require(input.getDayOfWeek() != null && input.getStartTime() != null && input.getEndTime() != null
                && input.getStartTime().isBefore(input.getEndTime()), "Jour et heures valides requis (sans passage à minuit)");
        Checks.require(input.getValidFrom() != null && input.getValidUntil() != null && !input.getValidFrom().isAfter(input.getValidUntil()), "Période de validité invalide");
        InstructorAvailability a = new InstructorAvailability();
        a.setInstructor(instructor); a.setDayOfWeek(input.getDayOfWeek()); a.setStartTime(input.getStartTime()); a.setEndTime(input.getEndTime());
        a.setValidFrom(input.getValidFrom()); a.setValidUntil(input.getValidUntil());
        return availabilities.save(a);
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<InstructorAvailability> availability(Long id) {
        Checks.id(id); if (!instructors.existsById(id)) throw BusinessException.missing(INSTRUCTOR_RESOURCE);
        return availabilities.findByInstructorNumInstructor(id);
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public PlanningSlot get(Long id) {
        Checks.id(id); return slots.findById(id).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
    }
    @Override
    public PlanningSlot create(Requests.Slot input) { return save(new PlanningSlot(), input); }
    @Override
    public PlanningSlot update(Long id, Requests.Slot input) {
        Checks.id(id);
        PlanningSlot current = slots.lockById(id).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
        if (current.getStatus() == SlotStatus.CANCELLED) throw BusinessException.conflict("Créneau annulé non modifiable");
        if (!current.getStartTime().isAfter(LocalDateTime.now())) throw BusinessException.conflict("Créneau commencé non modifiable");
        // Do not silently move a skier's confirmed appointment.
        if (!registrations.findByPlanningSlotId(id).isEmpty()) throw BusinessException.conflict("Créneau avec historique d'inscriptions non modifiable");
        return save(current, input);
    }
    private PlanningSlot save(PlanningSlot slot, Requests.Slot input) {
        Checks.id(input.getCourseId()); Checks.id(input.getInstructorId());
        Checks.require(input.getStartTime() != null && input.getEndTime() != null && input.getStartTime().isBefore(input.getEndTime())
                && input.getStartTime().isAfter(LocalDateTime.now()) && input.getStartTime().toLocalDate().equals(input.getEndTime().toLocalDate()), "Créneau futur, de durée positive et sur une même journée requis");
        Course course = courses.lockById(input.getCourseId()).orElseThrow(() -> BusinessException.missing(COURSE_RESOURCE));
        Instructor instructor = instructors.lockById(input.getInstructorId()).orElseThrow(() -> BusinessException.missing(INSTRUCTOR_RESOURCE));
        if (!instructor.getCourses().contains(course)) throw BusinessException.conflict("Ce moniteur n'est pas associé au cours");
        int ceiling = course.getTypeCourse() == TypeCourse.INDIVIDUAL ? 1 : 6;
        Checks.require(input.getMaxCapacity() != null && input.getMaxCapacity() > 0 && input.getMaxCapacity() <= ceiling,
                "Capacité requise : 1 en individuel, au plus 6 en collectif");
        boolean covered = availability(instructor.getNumInstructor()).stream().anyMatch(a -> a.getDayOfWeek() == input.getStartTime().getDayOfWeek()
                && !input.getStartTime().toLocalDate().isBefore(a.getValidFrom()) && !input.getEndTime().toLocalDate().isAfter(a.getValidUntil())
                && !input.getStartTime().toLocalTime().isBefore(a.getStartTime()) && !input.getEndTime().toLocalTime().isAfter(a.getEndTime()));
        if (!covered) throw BusinessException.conflict("Créneau hors des disponibilités du moniteur");
        boolean overlap = slots.findAll().stream().anyMatch(s -> !s.getId().equals(slot.getId()) && s.getStatus() != SlotStatus.CANCELLED
                && s.getInstructor().getNumInstructor().equals(input.getInstructorId())
                && TimeRanges.overlaps(s.getStartTime(), s.getEndTime(), input.getStartTime(), input.getEndTime()));
        if (overlap) throw BusinessException.conflict("Le moniteur a déjà un cours sur ce créneau");
        slot.setCourse(course); slot.setInstructor(instructor); slot.setStartTime(input.getStartTime()); slot.setEndTime(input.getEndTime());
        slot.setMaxCapacity(input.getMaxCapacity()); slot.setStatus(SlotStatus.OPEN);
        return slots.save(slot);
    }
    @Override
    public void cancel(Long id) {
        Checks.id(id);
        PlanningSlot slot = slots.lockById(id).orElseThrow(() -> BusinessException.missing(SLOT_RESOURCE));
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) throw BusinessException.conflict("Créneau commencé non annulable");
        slot.setStatus(SlotStatus.CANCELLED);
        registrations.findByPlanningSlotId(id).forEach(r -> r.setCancelled(true));
    }
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<PlanningSlot> search(LocalDateTime from, LocalDateTime to, Long courseId, Long instructorId, boolean available) {
        Checks.require(from != null && to != null && from.isBefore(to), "Période de recherche invalide");
        if (courseId != null) { Checks.id(courseId); if (!courses.existsById(courseId)) throw BusinessException.missing(COURSE_RESOURCE); }
        if (instructorId != null) { Checks.id(instructorId); if (!instructors.existsById(instructorId)) throw BusinessException.missing(INSTRUCTOR_RESOURCE); }
        return slots.findAll().stream().filter(s -> TimeRanges.overlaps(s.getStartTime(), s.getEndTime(), from, to)
                && (courseId == null || s.getCourse().getNumCourse().equals(courseId))
                && (instructorId == null || s.getInstructor().getNumInstructor().equals(instructorId))
                && (!available || (s.getStatus() == SlotStatus.OPEN && s.getStartTime().isAfter(LocalDateTime.now()))))
                .sorted(java.util.Comparator.comparing(PlanningSlot::getStartTime)).collect(Collectors.toList());
    }
}

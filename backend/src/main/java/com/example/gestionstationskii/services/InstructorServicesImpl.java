package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class InstructorServicesImpl implements IInstructorServices {

    private final IInstructorRepository instructorRepository;
    private final ICourseRepository courseRepository;
    private final PlanningSlotRepository slotRepository;
    private final InstructorAvailabilityRepository availabilityRepository;

    @Override
    public Instructor addInstructor(Instructor instructor) {
        log.debug("Adding instructor: firstName={}, lastName={}, dateOfHire={}",
                instructor.getFirstName(), instructor.getLastName(), instructor.getDateOfHire());
        Checks.require(instructor.getNumInstructor() == null, "Identifiant généré à la création");
        validate(instructor);
        Checks.require(instructor.getCourses() == null || instructor.getCourses().isEmpty(), "Utiliser la route d'affectation des cours");
        instructor.setCourses(new HashSet<>());
        Instructor saved = instructorRepository.save(instructor);
        log.info("Instructor created with id={}", saved.getNumInstructor());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> retrieveAllInstructors() {
        log.debug("Retrieving all instructors");
        List<Instructor> instructors = instructorRepository.findAll();
        log.info("{} instructor(s) retrieved", instructors.size());
        return instructors;
    }

    @Override
    public Instructor updateInstructor(Instructor instructor) {
        log.debug("Updating instructor id={}", instructor.getNumInstructor());
        validate(instructor);
        Instructor current = findInstructor(instructor.getNumInstructor());
        current.setFirstName(instructor.getFirstName());
        current.setLastName(instructor.getLastName());
        current.setDateOfHire(instructor.getDateOfHire());
        Instructor saved = instructorRepository.save(current);
        log.info("Instructor id={} updated", saved.getNumInstructor());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Instructor retrieveInstructor(Long numInstructor) {
        return findInstructor(numInstructor);
    }

    private Instructor findInstructor(Long numInstructor) {
        log.debug("Retrieving instructor id={}", numInstructor);
        Checks.id(numInstructor);
        return instructorRepository.findById(numInstructor).orElseThrow(() -> {
            log.warn("Instructor id={} not found", numInstructor);
            return BusinessException.missing("Moniteur");
        });
    }

    @Override
    public Instructor addInstructorAndAssignToCourse(Instructor instructor, Long numCourse) {
        log.debug("Adding instructor and assigning to course id={}", numCourse);
        Instructor saved = addInstructor(instructor);
        Instructor assigned = assignCourse(saved.getNumInstructor(), numCourse);
        log.info("Instructor id={} created and assigned to course id={}", saved.getNumInstructor(), numCourse);
        return assigned;
    }

    private void validate(Instructor instructor) {
        log.debug("Validating instructor: firstName={}, lastName={}, dateOfHire={}",
                instructor.getFirstName(), instructor.getLastName(), instructor.getDateOfHire());
        Checks.text(instructor.getFirstName(), "Prénom");
        Checks.text(instructor.getLastName(), "Nom");
        Checks.require(instructor.getDateOfHire() != null && !instructor.getDateOfHire().isAfter(LocalDate.now(ZoneId.systemDefault())),
                "Date d'embauche présente ou passée requise");
    }

    @Override
    public Instructor assignCourse(Long id, Long courseId) {
        log.debug("Assigning course id={} to instructor id={}", courseId, id);
        Checks.id(id);
        Checks.id(courseId);
        Course course = courseRepository.lockById(courseId).orElseThrow(() -> {
            log.warn("Assignment refused: course id={} not found", courseId);
            return BusinessException.missing("Cours");
        });
        Instructor instructor = findInstructor(id);
        if (!instructorRepository.findByCourses_NumCourse(courseId).isEmpty()) {
            log.warn("Assignment refused: course id={} already has an instructor", courseId);
            throw BusinessException.conflict("Cours déjà associé à un moniteur");
        }
        instructor.getCourses().add(course);
        Instructor saved = instructorRepository.save(instructor);
        log.info("Course id={} assigned to instructor id={}", courseId, id);
        return saved;
    }

    @Override
    public Instructor unassignCourse(Long id, Long courseId) {
        log.debug("Unassigning course id={} from instructor id={}", courseId, id);
        Checks.id(courseId);
        Course course = courseRepository.lockById(courseId).orElseThrow(() -> {
            log.warn("Unassignment refused: course id={} not found", courseId);
            return BusinessException.missing("Cours");
        });
        Instructor instructor = findInstructor(id);
        if (slotRepository.findAll().stream().anyMatch(s -> s.getCourse().getNumCourse().equals(courseId)
                && s.getInstructor().getNumInstructor().equals(id)
                && s.getStatus() != SlotStatus.CANCELLED
                && s.getEndTime().isAfter(LocalDateTime.now(ZoneId.systemDefault())))) {
            log.warn("Unassignment refused: instructor id={} has future slots for course id={}", id, courseId);
            throw BusinessException.conflict("Annuler les créneaux futurs avant de retirer le cours");
        }
        if (!instructor.getCourses().remove(course)) {
            log.warn("Unassignment refused: no link between instructor id={} and course id={}", id, courseId);
            throw BusinessException.conflict("Association inexistante");
        }
        Instructor saved = instructorRepository.save(instructor);
        log.info("Course id={} unassigned from instructor id={}", courseId, id);
        return saved;
    }

    @Override
    public void removeInstructor(Long id) {
        log.debug("Removing instructor id={}", id);
        Instructor instructor = findInstructor(id);
        if (!instructor.getCourses().isEmpty()
                || slotRepository.existsByInstructor_NumInstructor(id)
                || availabilityRepository.existsByInstructor_NumInstructor(id)) {
            log.warn("Removal refused: instructor id={} is linked to courses, slots or availabilities", id);
            throw BusinessException.conflict("Moniteur associé à des cours, créneaux ou disponibilités : suppression refusée");
        }
        instructorRepository.delete(instructor);
        log.info("Instructor id={} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Course> courses(Long id) {
        log.debug("Retrieving courses of instructor id={}", id);
        Set<Course> courses = findInstructor(id).getCourses();
        log.info("{} course(s) found for instructor id={}", courses.size(), id);
        return courses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> search(String name) {
        log.debug("Searching instructors by name='{}'", name);
        String query = name.toLowerCase(java.util.Locale.ROOT);
        List<Instructor> result = instructorRepository.findAll().stream()
                .filter(i -> (i.getFirstName() + " " + i.getLastName()).toLowerCase(java.util.Locale.ROOT).contains(query))
                .collect(java.util.stream.Collectors.toList());
        log.info("Search returned {} instructor(s)", result.size());
        return result;
    }
}
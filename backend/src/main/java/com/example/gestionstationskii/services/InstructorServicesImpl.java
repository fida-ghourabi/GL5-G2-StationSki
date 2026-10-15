package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import com.example.gestionstationskii.repositories.IInstructorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional
public class InstructorServicesImpl implements IInstructorServices{

    private IInstructorRepository instructorRepository;
    private ICourseRepository courseRepository;
    private PlanningSlotRepository slotRepository;
    private InstructorAvailabilityRepository availabilityRepository;

    @Override
    public Instructor addInstructor(Instructor instructor) {
        Checks.require(instructor.getNumInstructor() == null, "Identifiant généré à la création");
        validate(instructor);
        Checks.require(instructor.getCourses() == null || instructor.getCourses().isEmpty(), "Utiliser la route d'affectation des cours");
        instructor.setCourses(new HashSet<>());
        return instructorRepository.save(instructor);
    }

    @Override
    public List<Instructor> retrieveAllInstructors() {
        return instructorRepository.findAll();
    }

    @Override
    public Instructor updateInstructor(Instructor instructor) {
        validate(instructor);
        Instructor current = retrieveInstructor(instructor.getNumInstructor());
        current.setFirstName(instructor.getFirstName()); current.setLastName(instructor.getLastName()); current.setDateOfHire(instructor.getDateOfHire());
        return instructorRepository.save(current);
    }

    @Override
    public Instructor retrieveInstructor(Long numInstructor) {
        Checks.id(numInstructor);
        return instructorRepository.findById(numInstructor).orElseThrow(() -> BusinessException.missing("Moniteur"));
    }

    @Override
    public Instructor addInstructorAndAssignToCourse(Instructor instructor, Long numCourse) {
        Instructor saved = addInstructor(instructor);
        return assignCourse(saved.getNumInstructor(), numCourse);
    }

    private void validate(Instructor instructor) {
        Checks.text(instructor.getFirstName(), "Prénom"); Checks.text(instructor.getLastName(), "Nom");
        Checks.require(instructor.getDateOfHire() != null && !instructor.getDateOfHire().isAfter(java.time.LocalDate.now()), "Date d'embauche présente ou passée requise");
    }
    public Instructor assignCourse(Long id, Long courseId) {
        Checks.id(id); Checks.id(courseId);
        Course course = courseRepository.lockById(courseId).orElseThrow(() -> BusinessException.missing("Cours"));
        Instructor instructor = retrieveInstructor(id);
        if (!instructorRepository.findByCourses_NumCourse(courseId).isEmpty()) throw BusinessException.conflict("Cours déjà associé à un moniteur");
        instructor.getCourses().add(course);
        return instructorRepository.save(instructor);
    }
    public Instructor unassignCourse(Long id, Long courseId) {
        Checks.id(courseId);
        Course course = courseRepository.lockById(courseId).orElseThrow(() -> BusinessException.missing("Cours"));
        Instructor instructor = retrieveInstructor(id);
        if (slotRepository.findAll().stream().anyMatch(s -> s.getCourse().getNumCourse().equals(courseId)
                && s.getInstructor().getNumInstructor().equals(id) && s.getStatus() != SlotStatus.CANCELLED && s.getEndTime().isAfter(java.time.LocalDateTime.now())))
            throw BusinessException.conflict("Annuler les créneaux futurs avant de retirer le cours");
        if (!instructor.getCourses().remove(course)) throw BusinessException.conflict("Association inexistante");
        return instructorRepository.save(instructor);
    }
    public void removeInstructor(Long id) {
        Instructor instructor = retrieveInstructor(id);
        if (!instructor.getCourses().isEmpty() || slotRepository.existsByInstructor_NumInstructor(id) || availabilityRepository.existsByInstructor_NumInstructor(id))
            throw BusinessException.conflict("Moniteur associé à des cours, créneaux ou disponibilités : suppression refusée");
        instructorRepository.delete(instructor);
    }
    public Set<Course> courses(Long id) { return retrieveInstructor(id).getCourses(); }
    public List<Instructor> search(String name) {
        String query = name.toLowerCase(java.util.Locale.ROOT);
        return instructorRepository.findAll().stream().filter(i -> (i.getFirstName() + " " + i.getLastName()).toLowerCase(java.util.Locale.ROOT).contains(query))
                .collect(java.util.stream.Collectors.toList());
    }


}

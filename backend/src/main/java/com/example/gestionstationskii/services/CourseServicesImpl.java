package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@RequiredArgsConstructor
@Service
@Transactional
public class CourseServicesImpl implements ICourseServices {

    private final ICourseRepository courseRepository;
    private final IRegistrationRepository registrationRepository;
    private final PlanningSlotRepository slotRepository;
    private final IInstructorRepository instructorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Course> retrieveAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Course addCourse(Course course) {
        Checks.require(course.getNumCourse() == null, "Identifiant généré à la création");
        validate(course);
        course.setRegistrations(new java.util.HashSet<>());
        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Course course) {
        validate(course);
        Checks.id(course.getNumCourse());
        Course current = courseRepository.lockById(course.getNumCourse()).orElseThrow(() -> BusinessException.missing("Cours"));
        if ((registrationRepository.existsByCourse_NumCourse(current.getNumCourse()) || slotRepository.existsByCourse_NumCourse(current.getNumCourse()))
                && (current.getTypeCourse() != course.getTypeCourse() || current.getSupport() != course.getSupport() || current.getLevel() != course.getLevel()))
            throw BusinessException.conflict("Type, support et niveau immuables pour un cours planifié ou ayant des inscriptions");
        current.setLevel(course.getLevel()); current.setTypeCourse(course.getTypeCourse()); current.setSupport(course.getSupport());
        current.setPrice(course.getPrice()); current.setTimeSlot(course.getTimeSlot());
        return courseRepository.save(current);
    }

    @Override
    @Transactional(readOnly = true)
    public Course retrieveCourse(Long numCourse) {
        Checks.id(numCourse);
        return courseRepository.findById(numCourse).orElseThrow(() -> BusinessException.missing("Cours"));
    }

    private void validate(Course course) {
        Checks.require(course.getLevel() > 0 && course.getTypeCourse() != null && course.getSupport() != null, "Niveau positif, type et support requis");
        Checks.price(course.getPrice());
        Checks.require(course.getTimeSlot() >= 0, "Ancien timeSlot positif ou nul requis");
    }
    @Override
    public void removeCourse(Long id) {
        Course course = retrieveCourse(id);
        if (registrationRepository.existsByCourse_NumCourse(id) || slotRepository.existsByCourse_NumCourse(id)
                || !instructorRepository.findByCourses_NumCourse(id).isEmpty())
            throw BusinessException.conflict("Cours associé à un moniteur, des inscriptions ou des créneaux : suppression refusée");
        courseRepository.delete(course);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Course> search(TypeCourse type, Support support, Integer level) {
        if (level != null) Checks.require(level > 0, "Niveau positif requis");
        return courseRepository.findAll().stream().filter(c -> (type == null || c.getTypeCourse() == type)
                && (support == null || c.getSupport() == support) && (level == null || c.getLevel() == level))
                .collect(java.util.stream.Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<Registration> registrations(Long id) { retrieveCourse(id); return registrationRepository.findByCourse_NumCourse(id); }
    @Override
    @Transactional(readOnly = true)
    public long participants(Long id) {
        return registrations(id).stream().filter(r -> !r.isCancelled()).map(r -> r.getSkier().getNumSkier()).distinct().count();
    }
    @Override
    @Transactional(readOnly = true)
    public List<Instructor> instructors(Long id) { retrieveCourse(id); return instructorRepository.findByCourses_NumCourse(id); }


}

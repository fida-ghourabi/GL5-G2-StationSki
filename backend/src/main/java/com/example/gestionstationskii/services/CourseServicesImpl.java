package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
        log.debug("Retrieving all courses");
        List<Course> courses = courseRepository.findAll();
        log.info("{} course(s) retrieved", courses.size());
        return courses;
    }

    @Override
    public Course addCourse(Course course) {
        log.debug("Adding course: type={}, support={}, level={}, price={}",
                course.getTypeCourse(), course.getSupport(), course.getLevel(), course.getPrice());
        Checks.require(course.getNumCourse() == null, "Identifiant généré à la création");
        validate(course);
        course.setRegistrations(new java.util.HashSet<>());
        Course saved = courseRepository.save(course);
        log.info("Course created with id={}", saved.getNumCourse());
        return saved;
    }

    @Override
    public Course updateCourse(Course course) {
        log.debug("Updating course id={}", course.getNumCourse());
        validate(course);
        Checks.id(course.getNumCourse());
        Course current = courseRepository.lockById(course.getNumCourse()).orElseThrow(() -> {
            log.warn("Update refused: course id={} not found", course.getNumCourse());
            return BusinessException.missing("Cours");
        });
        if ((registrationRepository.existsByCourse_NumCourse(current.getNumCourse())
                || slotRepository.existsByCourse_NumCourse(current.getNumCourse()))
                && (current.getTypeCourse() != course.getTypeCourse()
                || current.getSupport() != course.getSupport()
                || current.getLevel() != course.getLevel())) {
            log.warn("Update refused: course id={} has registrations or slots, type/support/level are immutable",
                    current.getNumCourse());
            throw BusinessException.conflict("Type, support et niveau immuables pour un cours planifié ou ayant des inscriptions");
        }
        current.setLevel(course.getLevel());
        current.setTypeCourse(course.getTypeCourse());
        current.setSupport(course.getSupport());
        current.setPrice(course.getPrice());
        current.setTimeSlot(course.getTimeSlot());
        Course saved = courseRepository.save(current);
        log.info("Course id={} updated", saved.getNumCourse());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Course retrieveCourse(Long numCourse) {
        return findCourse(numCourse);
    }

    private Course findCourse(Long numCourse) {
        log.debug("Retrieving course id={}", numCourse);
        Checks.id(numCourse);
        return courseRepository.findById(numCourse).orElseThrow(() -> {
            log.warn("Course id={} not found", numCourse);
            return BusinessException.missing("Cours");
        });
    }

    private void validate(Course course) {
        log.debug("Validating course: level={}, type={}, support={}, price={}, timeSlot={}",
                course.getLevel(), course.getTypeCourse(), course.getSupport(),
                course.getPrice(), course.getTimeSlot());
        Checks.require(course.getLevel() > 0 && course.getTypeCourse() != null && course.getSupport() != null,
                "Niveau positif, type et support requis");
        Checks.price(course.getPrice());
        Checks.require(course.getTimeSlot() >= 0, "Ancien timeSlot positif ou nul requis");
    }

    @Override
    public void removeCourse(Long id) {
        log.debug("Removing course id={}", id);
        Course course = findCourse(id);
        if (registrationRepository.existsByCourse_NumCourse(id)
                || slotRepository.existsByCourse_NumCourse(id)
                || !instructorRepository.findByCourses_NumCourse(id).isEmpty()) {
            log.warn("Removal refused: course id={} is linked to an instructor, registrations or slots", id);
            throw BusinessException.conflict("Cours associé à un moniteur, des inscriptions ou des créneaux : suppression refusée");
        }
        courseRepository.delete(course);
        log.info("Course id={} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> search(TypeCourse type, Support support, Integer level) {
        log.debug("Searching courses: type={}, support={}, level={}", type, support, level);
        if (level != null) Checks.require(level > 0, "Niveau positif requis");
        List<Course> result = courseRepository.findAll().stream()
                .filter(c -> (type == null || c.getTypeCourse() == type)
                        && (support == null || c.getSupport() == support)
                        && (level == null || c.getLevel() == level))
                .collect(java.util.stream.Collectors.toList());
        log.info("Search returned {} course(s)", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> registrations(Long id) {
        return findRegistrations(id);
    }

    private List<Registration> findRegistrations(Long id) {
        log.debug("Retrieving registrations of course id={}", id);
        findCourse(id);
        List<Registration> registrations = registrationRepository.findByCourse_NumCourse(id);
        log.info("{} registration(s) found for course id={}", registrations.size(), id);
        return registrations;
    }

    @Override
    @Transactional(readOnly = true)
    public long participants(Long id) {
        log.debug("Counting participants of course id={}", id);
        long count = findRegistrations(id).stream()
                .filter(r -> !r.isCancelled())
                .map(r -> r.getSkier().getNumSkier())
                .distinct()
                .count();
        log.info("Course id={} has {} active participant(s)", id, count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> instructors(Long id) {
        log.debug("Retrieving instructors of course id={}", id);
        findCourse(id);
        List<Instructor> instructors = instructorRepository.findByCourses_NumCourse(id);
        log.info("{} instructor(s) found for course id={}", instructors.size(), id);
        return instructors;
    }
}
package com.example.gestionstationskii.repositories;

import com.example.gestionstationskii.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:registration-repository;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RegistrationRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private IRegistrationRepository registrations;

    private Skier skier;
    private Course course;
    private Instructor instructor;
    private PlanningSlot slot;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        course.setSupport(Support.SKI);
        course.setLevel(1);
        course.setPrice(50f);
        entityManager.persist(course);

        instructor = new Instructor();
        instructor.setCourses(Collections.singleton(course));
        entityManager.persist(instructor);

        skier = new Skier();
        skier.setDateOfBirth(LocalDate.of(1990, 1, 1));
        entityManager.persist(skier);

        slot = new PlanningSlot();
        slot.setCourse(course);
        slot.setInstructor(instructor);
        slot.setStartTime(LocalDateTime.of(2027, 1, 5, 10, 0));
        slot.setEndTime(LocalDateTime.of(2027, 1, 5, 11, 0));
        slot.setMaxCapacity(6);
        slot.setStatus(SlotStatus.OPEN);
        entityManager.persist(slot);
    }

    @Test
    void persistsAndReloadsRegistrationWithItsSkierCourseAndSlotRelations() {
        Registration registration = saveRegistration(false, 2);
        Long id = registration.getNumRegistration();
        entityManager.flush();
        entityManager.clear();

        Registration loaded = registrations.findById(id).orElseThrow(AssertionError::new);

        assertNotNull(loaded.getNumRegistration());
        assertEquals(skier.getNumSkier(), loaded.getSkier().getNumSkier());
        assertEquals(course.getNumCourse(), loaded.getCourse().getNumCourse());
        assertEquals(slot.getId(), loaded.getPlanningSlot().getId());
        assertFalse(loaded.isCancelled());
        assertEquals(2, loaded.getNumWeek());
    }

    @Test
    void findsRegistrationBySkierCourseAndSlotAndCountsBySlot() {
        Registration active = saveRegistration(false, 2);
        Registration cancelled = saveRegistration(true, 3);
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, registrations.findBySkier_NumSkier(skier.getNumSkier()).size());
        assertEquals(2, registrations.findByCourse_NumCourse(course.getNumCourse()).size());
        assertEquals(2, registrations.findByPlanningSlot_Id(slot.getId()).size());
        assertEquals(2, registrations.countByPlanningSlot_Id(slot.getId()));
        assertTrue(registrations.existsBySkier_NumSkier(skier.getNumSkier()));
        assertTrue(registrations.existsByCourse_NumCourse(course.getNumCourse()));
        assertEquals(1L, registrations.countByCourseAndNumWeek(course, 2));
        assertEquals(1L, registrations.countDistinctByNumWeekAndSkier_NumSkierAndCourse_NumCourse(
                2, skier.getNumSkier(), course.getNumCourse()));
        assertEquals(0L, registrations.countDistinctByNumWeekAndSkier_NumSkierAndCourse_NumCourse(
                2, 999L, course.getNumCourse()));
        assertNotEquals(active.getNumRegistration(), cancelled.getNumRegistration());
    }

    @Test
    void customInstructorWeekQueryExcludesCancelledRegistrations() {
        saveRegistration(false, 2);
        saveRegistration(false, 3);
        saveRegistration(true, 4);
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, registrations.numWeeksCourseOfInstructorBySupport(instructor.getNumInstructor(), Support.SKI).size());
        assertTrue(registrations.numWeeksCourseOfInstructorBySupport(instructor.getNumInstructor(), Support.SNOWBOARD).isEmpty());
    }

    private Registration saveRegistration(boolean cancelled, int week) {
        Registration registration = new Registration();
        registration.setSkier(skier);
        registration.setCourse(course);
        registration.setPlanningSlot(slot);
        registration.setCancelled(cancelled);
        registration.setNumWeek(week);
        entityManager.persist(registration);
        return registration;
    }
}

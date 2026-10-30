package com.example.gestionstationskii.services.registration;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.IRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RegistrationRepositoryTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("station_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
    }

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

        assertEquals(2, registrations.findBySkierNumSkier(skier.getNumSkier()).size());
        assertEquals(2, registrations.findByCourseNumCourse(course.getNumCourse()).size());
        assertEquals(2, registrations.findByPlanningSlotId(slot.getId()).size());
        assertEquals(2, registrations.countByPlanningSlotId(slot.getId()));
        assertTrue(registrations.existsBySkierNumSkier(skier.getNumSkier()));
        assertTrue(registrations.existsByCourseNumCourse(course.getNumCourse()));
        assertEquals(1L, registrations.countByCourseAndNumWeek(course, 2));
        assertEquals(1L, registrations.countDistinctByNumWeekAndSkierNumSkierAndCourseNumCourse(
                2, skier.getNumSkier(), course.getNumCourse()));
        assertEquals(0L, registrations.countDistinctByNumWeekAndSkierNumSkierAndCourseNumCourse(
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

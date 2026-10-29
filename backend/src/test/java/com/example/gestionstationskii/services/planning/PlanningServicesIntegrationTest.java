package com.example.gestionstationskii.services.planning;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPlanningServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SANS MOCK du planning : vrai Spring, vraie base MySQL (Testcontainers), aucun repository simulé.
 *
 * Les 11 premiers tests reprennent un à un ceux de PlanningServicesMockTest (même nom, même règle métier).
 * Les tests suivants complètent avec des règles que PlanningServicesMockTest ne couvre pas encore.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class PlanningServicesIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("planning_test")
            .withUsername("planning_test")
            .withPassword("planning_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private IPlanningServices service;
    @Autowired private ICourseRepository courses;
    @Autowired private IInstructorRepository instructors;
    @Autowired private InstructorAvailabilityRepository availabilities;
    @Autowired private PlanningSlotRepository slots;
    @Autowired private IRegistrationRepository registrations;

    @BeforeEach
    void cleanPlanningData() {
        registrations.deleteAll();
        slots.deleteAll();
        availabilities.deleteAll();
        instructors.deleteAll();
        courses.deleteAll();
    }

    // =====================================================================================
    //  Les 11 tests « miroirs » de PlanningServicesMockTest
    // =====================================================================================

    /** 1. addAvailabilityPersistsTheInstructorSchedule */
    @Test
    void addAvailabilityPersistsTheInstructorSchedule() {
        Fixture fixture = createFixture(TypeCourse.COLLECTIVE_ADULT, true, false);
        Requests.Availability input = availabilityInput();

        InstructorAvailability saved = service.addAvailability(fixture.instructor.getNumInstructor(), input);

        assertNotNull(saved.getId());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), saved.getStartTime());
        assertTrue(availabilities.findById(saved.getId()).isPresent());
        assertEquals(1, service.availability(fixture.instructor.getNumInstructor()).size());
    }

    /** 2. addAvailabilityRejectsInvalidTimeRange */
    @Test
    void addAvailabilityRejectsInvalidTimeRange() {
        Fixture fixture = createFixture(TypeCourse.COLLECTIVE_ADULT, true, false);
        Requests.Availability input = availabilityInput();
        input.setStartTime(LocalTime.of(12, 0));
        input.setEndTime(LocalTime.of(9, 0));
        Long instructorId = fixture.instructor.getNumInstructor();

        assertThrows(BusinessException.class, () -> service.addAvailability(instructorId, input));

        assertEquals(0, availabilities.count());
    }

    /** 3. availabilityRejectsUnknownInstructor */
    @Test
    void availabilityRejectsUnknownInstructor() {
        assertThrows(BusinessException.class, () -> service.availability(987654L));
    }

    /** 4. getReturnsSlotAndRejectsMissingSlot */
    @Test
    void getReturnsSlotAndRejectsMissingSlot() {
        Fixture fixture = createFixture();
        PlanningSlot created = service.create(slotInput(fixture, 10, 6));

        PlanningSlot loaded = service.get(created.getId());

        assertEquals(created.getId(), loaded.getId());
        assertThrows(BusinessException.class, () -> service.get(987654L));
    }

    /** 5. createPersistsAValidSlotWithinInstructorAvailability */
    @Test
    void createPersistsAValidSlotWithinInstructorAvailability() {
        Fixture fixture = createFixture();

        PlanningSlot saved = service.create(slotInput(fixture, 10, 6));

        PlanningSlot stored = slots.findById(saved.getId()).orElseThrow(AssertionError::new);
        assertEquals(SlotStatus.OPEN, stored.getStatus());
        assertEquals(6, stored.getMaxCapacity());
        assertEquals(fixture.course.getNumCourse(), stored.getCourse().getNumCourse());
        assertEquals(fixture.instructor.getNumInstructor(), stored.getInstructor().getNumInstructor());
    }

    /** 6. createRejectsCapacityAboveCollectiveLimit */
    @Test
    void createRejectsCapacityAboveCollectiveLimit() {
        Fixture fixture = createFixture();
        Requests.Slot input = slotInput(fixture, 10, 7);

        assertThrows(BusinessException.class, () -> service.create(input));

        assertEquals(0, slots.count());
    }

    /** 7. createRejectsInstructorNotAssignedToCourse */
    @Test
    void createRejectsInstructorNotAssignedToCourse() {
        Fixture fixture = createFixture(TypeCourse.COLLECTIVE_ADULT, false, true);
        Requests.Slot input = slotInput(fixture, 10, 6);

        assertThrows(BusinessException.class, () -> service.create(input));

        assertEquals(0, slots.count());
    }

    /** 8. updateRejectsSlotWithRegistrationHistory */
    @Test
    void updateRejectsSlotWithRegistrationHistory() {
        Fixture fixture = createFixture();
        PlanningSlot slot = service.create(slotInput(fixture, 10, 6));
        Registration registration = new Registration();
        registration.setCourse(fixture.course);
        registration.setPlanningSlot(slot);
        registrations.saveAndFlush(registration);
        Long slotId = slot.getId();
        Requests.Slot update = slotInput(fixture, 11, 5);

        assertThrows(BusinessException.class, () -> service.update(slotId, update));

        assertEquals(LocalTime.of(10, 0), slots.findById(slot.getId()).get().getStartTime().toLocalTime());
    }

    /** 9. updateRejectsCancelledOrStartedSlot */
    @Test
    void updateRejectsCancelledOrStartedSlot() {
        Fixture fixture = createFixture();
        PlanningSlot cancelled = service.create(slotInput(fixture, 10, 6));
        service.cancel(cancelled.getId());
        PlanningSlot started = new PlanningSlot();
        started.setCourse(fixture.course);
        started.setInstructor(fixture.instructor);
        started.setStartTime(LocalDateTime.now().minusHours(2));
        started.setEndTime(LocalDateTime.now().minusHours(1));
        started.setMaxCapacity(6);
        started.setStatus(SlotStatus.OPEN);
        started = slots.saveAndFlush(started);
        Long startedId = started.getId();
        Long cancelledId = cancelled.getId();
        Requests.Slot update = slotInput(fixture, 11, 5);

        assertThrows(BusinessException.class, () -> service.update(cancelledId, update));
        assertThrows(BusinessException.class, () -> service.update(startedId, update));
    }

    /** 10. cancelMarksSlotAndItsRegistrationsAsCancelled */
    @Test
    void cancelMarksSlotAndItsRegistrationsAsCancelled() {
        Fixture fixture = createFixture();
        PlanningSlot slot = service.create(slotInput(fixture, 10, 6));
        Registration registration = new Registration();
        registration.setCourse(fixture.course);
        registration.setPlanningSlot(slot);
        registration = registrations.saveAndFlush(registration);

        service.cancel(slot.getId());

        assertEquals(SlotStatus.CANCELLED, slots.findById(slot.getId()).get().getStatus());
        assertTrue(registrations.findById(registration.getNumRegistration()).get().isCancelled());
    }

    /** 11. searchFiltersAndSortsSlotsByStartTime */
    @Test
    void searchFiltersAndSortsSlotsByStartTime() {
        Fixture selected = createFixture();
        Fixture other = createFixture();
        PlanningSlot later = service.create(slotInput(selected, 11, 6));       // créé en premier : doit sortir en second
        PlanningSlot earlier = service.create(slotInput(selected, 9, 6));
        service.create(slotInput(other, 10, 6));                                // autre cours et autre moniteur : exclu
        LocalDateTime from = futureMonday().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<PlanningSlot> result = service.search(from, to, selected.course.getNumCourse(),
                selected.instructor.getNumInstructor(), false);

        assertEquals(2, result.size());
        assertEquals(earlier.getId(), result.get(0).getId());
        assertEquals(later.getId(), result.get(1).getId());
        assertEquals(3, service.search(from, to, null, null, false).size());
    }

    // =====================================================================================
    //  Tes tests d'origine (conservés)
    // =====================================================================================

    @Test
    void availabilityAndCreatedSlotArePersistedAndSearchable() {
        Fixture fixture = createFixture();
        Requests.Slot input = slotInput(fixture, 10, 6);

        PlanningSlot created = service.create(input);
        PlanningSlot loaded = service.get(created.getId());
        List<PlanningSlot> results = service.search(input.getStartTime().minusMinutes(30),
                input.getEndTime().plusMinutes(30), fixture.course.getNumCourse(),
                fixture.instructor.getNumInstructor(), true);

        assertNotNull(created.getId());
        assertEquals(SlotStatus.OPEN, loaded.getStatus());
        assertEquals(fixture.course.getNumCourse(), loaded.getCourse().getNumCourse());
        assertEquals(1, results.size());
        assertEquals(created.getId(), results.get(0).getId());
        assertEquals(1, service.availability(fixture.instructor.getNumInstructor()).size());
    }

    @Test
    void updatePersistsNewSlotTimeWhenThereIsNoRegistrationHistory() {
        Fixture fixture = createFixture();
        PlanningSlot created = service.create(slotInput(fixture, 10, 6));
        Requests.Slot update = slotInput(fixture, 11, 5);

        PlanningSlot saved = service.update(created.getId(), update);

        assertEquals(update.getStartTime(), slots.findById(created.getId()).get().getStartTime());
        assertEquals(5, saved.getMaxCapacity());
    }

    // =====================================================================================
    //  Règles métier que PlanningServicesMockTest ne couvre pas encore
    // =====================================================================================

    @Test
    void createRejectsOverlapWithAnotherSlotOfTheSameInstructor() {
        Fixture fixture = createFixture();
        service.create(slotInput(fixture, 10, 6));
        Requests.Slot overlapping = slotInput(fixture, 10, 6);
        overlapping.setStartTime(overlapping.getStartTime().plusMinutes(30));
        overlapping.setEndTime(overlapping.getEndTime().plusMinutes(30));

        assertThrows(BusinessException.class, () -> service.create(overlapping));

        assertEquals(1, slots.count());
    }

    @Test
    void createRejectsSlotOutsideInstructorAvailability() {
        Fixture fixture = createFixture();
        Requests.Slot afterWindow = slotInput(fixture, 14, 6);                  // la disponibilité va de 9h à 13h
        Requests.Slot wrongDay = slotInput(fixture, 10, 6);
        wrongDay.setStartTime(wrongDay.getStartTime().plusDays(1));             // mardi : le moniteur n'est dispo que le lundi
        wrongDay.setEndTime(wrongDay.getEndTime().plusDays(1));

        assertThrows(BusinessException.class, () -> service.create(afterWindow));
        assertThrows(BusinessException.class, () -> service.create(wrongDay));

        assertEquals(0, slots.count());
    }

    @Test
    void createRejectsSlotInThePast() {
        Fixture fixture = createFixture();
        Requests.Slot past = slotInput(fixture, 10, 6);
        past.setStartTime(LocalDateTime.now().minusDays(1));
        past.setEndTime(LocalDateTime.now().minusDays(1).plusHours(1));

        assertThrows(BusinessException.class, () -> service.create(past));
    }

    @Test
    void individualCourseAcceptsOnlyOnePlace() {
        Fixture fixture = createFixture(TypeCourse.INDIVIDUAL, true, true);
        Requests.Slot invalidCapacity = slotInput(fixture, 10, 2);

        assertThrows(BusinessException.class, () -> service.create(invalidCapacity));
        assertNotNull(service.create(slotInput(fixture, 10, 1)).getId());
    }

    // =====================================================================================
    //  Données de test
    // =====================================================================================

    private Fixture createFixture() {
        return createFixture(TypeCourse.COLLECTIVE_ADULT, true, true);
    }

    /**
     * @param assignedToCourse  le moniteur enseigne-t-il ce cours ?
     * @param withAvailability  le moniteur a-t-il déjà une disponibilité le lundi de 9h à 13h ?
     */
    private Fixture createFixture(TypeCourse type, boolean assignedToCourse, boolean withAvailability) {
        Course course = new Course();
        course.setTypeCourse(type);
        course.setSupport(Support.SKI);
        course.setLevel(1);
        course.setPrice(100F);
        course = courses.saveAndFlush(course);

        Instructor instructor = new Instructor();
        instructor.setFirstName("Planning");
        instructor.setLastName("Test");
        instructor.setCourses(assignedToCourse ? new HashSet<Course>(Collections.singleton(course)) : new HashSet<Course>());
        instructor = instructors.saveAndFlush(instructor);

        if (withAvailability) {
            LocalDate date = futureMonday();
            InstructorAvailability availability = new InstructorAvailability();
            availability.setInstructor(instructor);
            availability.setDayOfWeek(DayOfWeek.MONDAY);
            availability.setStartTime(LocalTime.of(9, 0));
            availability.setEndTime(LocalTime.of(13, 0));
            availability.setValidFrom(date.minusDays(7));
            availability.setValidUntil(date.plusDays(7));
            availabilities.saveAndFlush(availability);
        }
        return new Fixture(course, instructor);
    }

    private static Requests.Availability availabilityInput() {
        Requests.Availability input = new Requests.Availability();
        input.setDayOfWeek(DayOfWeek.MONDAY);
        input.setStartTime(LocalTime.of(9, 0));
        input.setEndTime(LocalTime.of(13, 0));
        input.setValidFrom(futureMonday().minusDays(7));
        input.setValidUntil(futureMonday().plusDays(7));
        return input;
    }

    /** Créneau d'une heure, le lundi, à l'heure donnée. */
    private static Requests.Slot slotInput(Fixture fixture, int startHour, int capacity) {
        Requests.Slot input = new Requests.Slot();
        input.setCourseId(fixture.course.getNumCourse());
        input.setInstructorId(fixture.instructor.getNumInstructor());
        LocalDate date = futureMonday();
        input.setStartTime(date.atTime(startHour, 0));
        input.setEndTime(date.atTime(startHour + 1, 0));
        input.setMaxCapacity(capacity);
        return input;
    }

    private static LocalDate futureMonday() {
        return LocalDate.now().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private static final class Fixture {
        private final Course course;
        private final Instructor instructor;

        private Fixture(Course course, Instructor instructor) {
            this.course = course;
            this.instructor = instructor;
        }
    }
}
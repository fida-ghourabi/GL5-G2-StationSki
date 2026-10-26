package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServicesMockTest {
    @Mock private IRegistrationRepository registrations;
    @Mock private ISkierRepository skiers;
    @Mock private ICourseRepository courses;
    @Mock private PlanningSlotRepository slots;
    @Mock private IInstructorRepository instructors;
    @Mock private EntityManager entityManager;
    @InjectMocks private RegistrationServicesImpl service;

    private Skier adult;
    private Course adultCourse;
    private PlanningSlot openSlot;

    @BeforeEach
    void setUp() {
        adult = skier(10L, LocalDate.of(1990, 1, 1));
        adultCourse = course(20L, TypeCourse.COLLECTIVE_ADULT);
        openSlot = slot(30L, adultCourse, LocalDateTime.now().plusDays(4), 2, SlotStatus.OPEN);
    }

    @Test
    void registerSlotPersistsRegistrationAndMarksLastSeatFull() {
        openSlot.setMaxCapacity(1);
        prepareRegistration(adult, openSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());
        when(registrations.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Registration result = service.registerSlot(10L, 30L);

        assertSame(adult, result.getSkier());
        assertSame(adultCourse, result.getCourse());
        assertSame(openSlot, result.getPlanningSlot());
        assertEquals(openSlot.getStartTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), result.getNumWeek());
        assertFalse(result.isCancelled());
        assertEquals(SlotStatus.FULL, openSlot.getStatus());
        verify(entityManager).refresh(openSlot);
        verify(registrations).save(result);
    }

    @Test
    void registerSlotRethrowsTechnicalFailureOnSave() {
        prepareRegistration(adult, openSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Registration save failed");
        when(registrations.save(any(Registration.class))).thenThrow(failure);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> service.registerSlot(10L, 30L));

        assertSame(failure, thrown);
    }

    @Test
    void registerSlotRejectsInvalidIdsBeforeRepositoryAccess() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.registerSlot(0L, 30L)).getStatus());
        verifyNoInteractions(skiers, slots);
    }

    @Test
    void registerSlotReportsMissingSkierOrSlot() {
        when(skiers.lockById(10L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.registerSlot(10L, 30L)).getStatus());
        verify(slots, never()).lockById(anyLong());

        when(skiers.lockById(10L)).thenReturn(Optional.of(adult));
        when(slots.lockById(30L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.registerSlot(10L, 30L)).getStatus());
    }

    @Test
    void registerSlotRejectsClosedAndAlreadyStartedSlots() {
        openSlot.setStatus(SlotStatus.CANCELLED);
        prepareRegistration(adult, openSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.registerSlot(10L, 30L)).getStatus());
        verify(registrations, never()).save(any(Registration.class));

        openSlot.setStatus(SlotStatus.OPEN);
        openSlot.setStartTime(LocalDateTime.now().minusMinutes(1));
        prepareRegistration(adult, openSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.registerSlot(10L, 30L)).getStatus());
    }

    @Test
    void registerSlotRequiresDateOfBirth() {
        adult.setDateOfBirth(null);
        prepareRegistration(adult, openSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());
        BusinessException error = assertThrows(BusinessException.class, () -> service.registerSlot(10L, 30L));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getMessage().contains("naissance"));
        verify(registrations, never()).save(any(Registration.class));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "16-year-old rejected from children course, 16, 0, COLLECTIVE_CHILDREN, false",
            "one day under 16 rejected from adult course, 16, 1, COLLECTIVE_ADULT, false",
            "exactly 16 accepted in adult course, 16, 0, COLLECTIVE_ADULT, true",
            "15-year-old accepted in children course, 15, 0, COLLECTIVE_CHILDREN, true",
            "5-year-old accepted in individual course, 5, 0, INDIVIDUAL, true"
    })
    void registerSlotAppliesAgeRules(String scenario, int age, int daysAfterBirthday,
                                     TypeCourse courseType, boolean accepted) {
        LocalDate lessonDate = openSlot.getStartTime().toLocalDate();
        Skier candidate = skier(11L, lessonDate.minusYears(age).plusDays(daysAfterBirthday));
        Course targetCourse = course(21L, courseType);
        PlanningSlot targetSlot = slot(31L, targetCourse, openSlot.getStartTime(), 3, SlotStatus.OPEN);
        prepareRegistration(candidate, targetSlot, Collections.<Registration>emptyList(), Collections.<Registration>emptyList());

        if (accepted) {
            when(registrations.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
            assertSame(candidate, service.registerSlot(candidate.getNumSkier(), targetSlot.getId()).getSkier());
        } else {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.registerSlot(candidate.getNumSkier(), targetSlot.getId()));
            assertEquals(HttpStatus.CONFLICT, error.getStatus());
            verify(registrations, never()).save(any(Registration.class));
        }
    }

    @Test
    void registerSlotRejectsOverlappingSkierAppointments() {
        PlanningSlot conflicting = slot(32L, course(22L, TypeCourse.COLLECTIVE_ADULT),
                openSlot.getStartTime().plusMinutes(30), 4, SlotStatus.OPEN);
        Registration existing = registration(adult, conflicting, conflicting.getCourse(), false);
        prepareRegistration(adult, openSlot, Collections.singletonList(existing), Collections.<Registration>emptyList());
        assertTrue(assertThrows(BusinessException.class, () -> service.registerSlot(10L, 30L))
                .getMessage().contains("conflit horaire"));
        verify(registrations, never()).save(any(Registration.class));
    }

    @Test
    void registerSlotRejectsAnotherLessonForSameCourseAndIsoWeek() {
        LocalDateTime monday = LocalDate.of(2030, 1, 7).atTime(10, 0);
        openSlot.setStartTime(monday);
        openSlot.setEndTime(monday.plusHours(1));
        PlanningSlot other = slot(33L, adultCourse, monday.plusHours(2), 4, SlotStatus.OPEN);
        Registration existing = registration(adult, other, adultCourse, false);
        prepareRegistration(adult, openSlot, Collections.singletonList(existing), Collections.<Registration>emptyList());
        assertTrue(assertThrows(BusinessException.class, () -> service.registerSlot(10L, 30L))
                .getMessage().contains("semaine ISO"));
    }

    @Test
    void registerSlotAllowsSameCourseAgainInAnotherIsoWeek() {
        LocalDateTime monday = LocalDate.of(2030, 1, 7).atTime(10, 0);
        openSlot.setStartTime(monday);
        openSlot.setEndTime(monday.plusHours(1));
        PlanningSlot nextWeek = slot(36L, adultCourse, monday.plusWeeks(1), 2, SlotStatus.OPEN);
        Registration previousWeek = registration(adult, openSlot, adultCourse, false);
        prepareRegistration(adult, nextWeek, Collections.singletonList(previousWeek), Collections.<Registration>emptyList());
        when(registrations.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Registration result = service.registerSlot(10L, 36L);

        assertSame(nextWeek, result.getPlanningSlot());
        assertNotEquals(openSlot.getStartTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), result.getNumWeek());
    }

    @Test
    void registerSlotIgnoresCancelledRegistrationAndRegistrationOnCancelledSlotForConflictChecks() {
        PlanningSlot cancelledRegistrationSlot = slot(40L, adultCourse, openSlot.getStartTime(), 2, SlotStatus.OPEN);
        PlanningSlot cancelledSlot = slot(41L, adultCourse, openSlot.getStartTime(), 2, SlotStatus.CANCELLED);
        Registration cancelledRegistration = registration(adult, cancelledRegistrationSlot, adultCourse, true);
        Registration registrationOnCancelledSlot = registration(adult, cancelledSlot, adultCourse, false);
        prepareRegistration(adult, openSlot, Arrays.asList(cancelledRegistration, registrationOnCancelledSlot),
                Collections.<Registration>emptyList());
        when(registrations.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Registration result = service.registerSlot(10L, 30L);

        assertSame(openSlot, result.getPlanningSlot());
    }

    @Test
    void registerSlotRejectsLegacyWeeklyDuplicateWithUnknownYear() {
        Registration historical = new Registration();
        historical.setSkier(adult);
        historical.setCourse(adultCourse);
        historical.setNumWeek(openSlot.getStartTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        prepareRegistration(adult, openSlot, Collections.singletonList(historical), Collections.<Registration>emptyList());
        assertTrue(assertThrows(BusinessException.class, () -> service.registerSlot(10L, 30L))
                .getMessage().contains("année inconnue"));
    }

    @Test
    void registerSlotCountsOnlyActiveRegistrationsAndRejectsFullCapacity() {
        PlanningSlot oneSeat = slot(34L, adultCourse, openSlot.getStartTime(), 1, SlotStatus.OPEN);
        Registration cancelled = registration(adult, oneSeat, adultCourse, true);
        prepareRegistration(adult, oneSeat, Collections.<Registration>emptyList(), Collections.singletonList(cancelled));
        when(registrations.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(SlotStatus.FULL, service.registerSlot(10L, 34L).getPlanningSlot().getStatus());

        PlanningSlot fullSlot = slot(35L, adultCourse, openSlot.getStartTime(), 1, SlotStatus.OPEN);
        prepareRegistration(adult, fullSlot, Collections.<Registration>emptyList(),
                Collections.singletonList(registration(adult, fullSlot, adultCourse, false)));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.registerSlot(10L, 35L)).getStatus());
    }

    @Test
    void nextAvailableSlotReturnsEarliestFutureOpenSlotForCourse() {
        PlanningSlot later = slot(37L, adultCourse, openSlot.getStartTime().plusDays(2), 2, SlotStatus.OPEN);
        PlanningSlot wrongCourse = slot(38L, course(23L, TypeCourse.COLLECTIVE_ADULT),
                openSlot.getStartTime().minusDays(1), 2, SlotStatus.OPEN);
        PlanningSlot full = slot(39L, adultCourse, openSlot.getStartTime().minusHours(1), 2, SlotStatus.FULL);
        when(slots.findAll()).thenReturn(Arrays.asList(later, wrongCourse, full, openSlot));

        assertSame(openSlot, service.nextAvailableSlot(20L));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.nextAvailableSlot(999L)).getStatus());
    }

    @Test
    void nextAvailableSlotRejectsWhenTheCourseHasNoFutureOpenSlot() {
        when(slots.findAll()).thenReturn(Collections.<PlanningSlot>emptyList());

        BusinessException error = assertThrows(BusinessException.class, () -> service.nextAvailableSlot(20L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void cancelMarksRegistrationAndReopensFullSlot() {
        openSlot.setStatus(SlotStatus.FULL);
        Registration registration = registration(adult, openSlot, adultCourse, false);
        registration.setNumRegistration(40L);
        when(registrations.findById(40L)).thenReturn(Optional.of(registration));
        when(skiers.lockById(10L)).thenReturn(Optional.of(adult));
        when(slots.lockById(30L)).thenReturn(Optional.of(openSlot));

        service.cancel(40L);

        assertTrue(registration.isCancelled());
        assertEquals(SlotStatus.OPEN, openSlot.getStatus());
        verify(entityManager).refresh(registration);
        verify(entityManager).refresh(openSlot);
    }

    @Test
    void cancelIsIdempotentForAlreadyCancelledRegistration() {
        Registration registration = registration(adult, openSlot, adultCourse, true);
        registration.setNumRegistration(41L);
        when(registrations.findById(41L)).thenReturn(Optional.of(registration));
        when(skiers.lockById(10L)).thenReturn(Optional.of(adult));
        when(slots.lockById(30L)).thenReturn(Optional.of(openSlot));

        assertDoesNotThrow(() -> service.cancel(41L));
        assertEquals(SlotStatus.OPEN, openSlot.getStatus());
    }

    @Test
    void cancelOnCancelledSlotDoesNotReopenIt() {
        openSlot.setStatus(SlotStatus.CANCELLED);
        Registration registration = registration(adult, openSlot, adultCourse, false);
        registration.setNumRegistration(45L);
        when(registrations.findById(45L)).thenReturn(Optional.of(registration));
        when(skiers.lockById(10L)).thenReturn(Optional.of(adult));
        when(slots.lockById(30L)).thenReturn(Optional.of(openSlot));

        service.cancel(45L);

        assertTrue(registration.isCancelled());
        assertEquals(SlotStatus.CANCELLED, openSlot.getStatus());
    }

    @Test
    void cancelRejectsStartedLessonsAndSupportsHistoricalRegistrationWithoutSlot() {
        Registration started = registration(adult, openSlot, adultCourse, false);
        started.setNumRegistration(42L);
        openSlot.setStartTime(LocalDateTime.now().minusMinutes(1));
        when(registrations.findById(42L)).thenReturn(Optional.of(started));
        when(skiers.lockById(10L)).thenReturn(Optional.of(adult));
        when(slots.lockById(30L)).thenReturn(Optional.of(openSlot));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.cancel(42L)).getStatus());
        assertFalse(started.isCancelled());

        Registration legacy = registration(adult, null, adultCourse, false);
        legacy.setNumRegistration(43L);
        when(registrations.findById(43L)).thenReturn(Optional.of(legacy));
        assertDoesNotThrow(() -> service.cancel(43L));
        assertTrue(legacy.isCancelled());
    }

    @Test
    void cancelRejectsMissingRegistration() {
        when(registrations.findById(44L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class, () -> service.cancel(44L)).getStatus());
    }

    @Test
    void bySkierValidatesSkierAndReturnsRepositoryHistory() {
        when(skiers.existsById(10L)).thenReturn(true);
        List<Registration> history = Collections.singletonList(registration(adult, openSlot, adultCourse, false));
        when(registrations.findBySkierNumSkier(10L)).thenReturn(history);

        assertSame(history, service.bySkier(10L));
        when(skiers.existsById(99L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class, () -> service.bySkier(99L)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class, () -> service.bySkier(0L)).getStatus());
    }

    @Test
    void searchFiltersByHalfOpenTimeRangeAndOptionalCourse() {
        LocalDateTime from = LocalDateTime.now().plusDays(5);
        LocalDateTime to = from.plusHours(2);
        PlanningSlot matches = slot(50L, adultCourse, from.plusMinutes(30), 2, SlotStatus.OPEN);
        PlanningSlot outside = slot(51L, adultCourse, to.plusMinutes(1), 2, SlotStatus.OPEN);
        Registration included = registration(adult, matches, adultCourse, false);
        Registration excludedByDate = registration(adult, outside, adultCourse, false);
        Registration historical = registration(adult, null, adultCourse, false);
        when(registrations.findAll()).thenReturn(Arrays.asList(included, excludedByDate, historical));

        assertEquals(Collections.singletonList(included), service.search(from, to, null));
        when(courses.existsById(20L)).thenReturn(true);
        assertEquals(Collections.singletonList(included), service.search(from, to, 20L));
    }

    @Test
    void searchRejectsInvalidRangeAndMissingCourse() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(1);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.search(from, from, null)).getStatus());
        when(courses.existsById(99L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.search(from, to, 99L)).getStatus());
    }

    @Test
    void addAndAssignToSkierRequiresNewRegistrationAndPlanningSlot() {
        Registration noSlot = new Registration();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.addRegistrationAndAssignToSkier(noSlot, 10L)).getStatus());
        noSlot.setNumRegistration(4L);
        noSlot.setPlanningSlot(openSlot);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.addRegistrationAndAssignToSkier(noSlot, 10L)).getStatus());
    }

    @Test
    void addAndAssignToCourseRejectsSlotFromDifferentCourse() {
        when(courses.existsById(60L)).thenReturn(true);
        when(slots.findById(30L)).thenReturn(Optional.of(openSlot));
        Registration input = new Registration();
        input.setPlanningSlot(openSlot);

        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.addRegistrationAndAssignToSkierAndCourse(input, 10L, 60L)).getStatus());
        verify(skiers, never()).lockById(anyLong());
    }

    @Test
    void assignRegistrationToCourseAllowsSameCourseButRejectsChangingCourse() {
        Registration existing = registration(adult, openSlot, adultCourse, false);
        when(registrations.findById(70L)).thenReturn(Optional.of(existing));
        when(courses.existsById(20L)).thenReturn(true);
        assertSame(existing, service.assignRegistrationToCourse(70L, 20L));

        when(courses.existsById(21L)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.assignRegistrationToCourse(70L, 21L)).getStatus());
        existing.setCancelled(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.assignRegistrationToCourse(70L, 20L)).getStatus());
    }

    @Test
    void instructorWeeksRequiresExistingInstructorAndSupport() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.numWeeksCourseOfInstructorBySupport(1L, null)).getStatus());
        when(instructors.existsById(1L)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.numWeeksCourseOfInstructorBySupport(1L, Support.SKI)).getStatus());
        when(instructors.existsById(1L)).thenReturn(true);
        when(registrations.numWeeksCourseOfInstructorBySupport(1L, Support.SKI)).thenReturn(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), service.numWeeksCourseOfInstructorBySupport(1L, Support.SKI));
    }

    private void prepareRegistration(Skier skier, PlanningSlot slot, List<Registration> skierHistory,
                                     List<Registration> slotRegistrations) {
        lenient().when(skiers.lockById(skier.getNumSkier())).thenReturn(Optional.of(skier));
        lenient().when(slots.lockById(slot.getId())).thenReturn(Optional.of(slot));
        lenient().when(registrations.findBySkierNumSkier(skier.getNumSkier())).thenReturn(skierHistory);
        lenient().when(registrations.findByPlanningSlotId(slot.getId())).thenReturn(slotRegistrations);
    }

    private static Skier skier(Long id, LocalDate dateOfBirth) {
        Skier skier = new Skier();
        skier.setNumSkier(id);
        skier.setDateOfBirth(dateOfBirth);
        return skier;
    }

    private static Course course(Long id, TypeCourse type) {
        Course course = new Course();
        course.setNumCourse(id);
        course.setTypeCourse(type);
        return course;
    }

    private static PlanningSlot slot(Long id, Course course, LocalDateTime start, int capacity, SlotStatus status) {
        PlanningSlot slot = new PlanningSlot();
        slot.setId(id);
        slot.setCourse(course);
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setMaxCapacity(capacity);
        slot.setStatus(status);
        return slot;
    }

    private static Registration registration(Skier skier, PlanningSlot slot, Course course, boolean cancelled) {
        Registration registration = new Registration();
        registration.setSkier(skier);
        registration.setPlanningSlot(slot);
        registration.setCourse(course);
        registration.setCancelled(cancelled);
        return registration;
    }
}

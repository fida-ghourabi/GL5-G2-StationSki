package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.gestionstationskii.services.BusinessAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServicesMockTest {

    @Mock private IRegistrationRepository registrationRepository;
    @Mock private ISkierRepository skierRepository;
    @Mock private ICourseRepository courseRepository;
    @Mock private PlanningSlotRepository slotRepository;
    @Mock private IInstructorRepository instructorRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private RegistrationServicesImpl service;

    private Skier skier;
    private Course course;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        // A Monday at least one week ahead: Monday and Wednesday of that week share the same ISO week.
        monday = LocalDate.now().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        skier = new Skier();
        skier.setNumSkier(1L);
        skier.setDateOfBirth(LocalDate.of(1995, 4, 12));

        course = new Course();
        course.setNumCourse(3L);
        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);

        when(skierRepository.lockById(1L)).thenReturn(Optional.of(skier));
        when(skierRepository.findById(1L)).thenReturn(Optional.of(skier));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(registrationRepository.findBySkier_NumSkier(1L)).thenReturn(Collections.<Registration>emptyList());
        when(registrationRepository.findByPlanningSlot_Id(anyLong())).thenReturn(Collections.<Registration>emptyList());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PlanningSlot slot(Long id, LocalDate date, int hour, int capacity, SlotStatus status) {
        PlanningSlot s = new PlanningSlot();
        s.setId(id);
        s.setCourse(course);
        s.setStartTime(date.atTime(hour, 0));
        s.setEndTime(date.atTime(hour + 1, 0));
        s.setMaxCapacity(capacity);
        s.setStatus(status);
        return s;
    }

    private PlanningSlot openSlot(Long id) {
        PlanningSlot s = slot(id, monday, 10, 4, SlotStatus.OPEN);
        when(slotRepository.lockById(id)).thenReturn(Optional.of(s));
        when(slotRepository.findById(id)).thenReturn(Optional.of(s));
        return s;
    }

    private Registration registration(Long id, PlanningSlot slot, boolean cancelled) {
        Registration r = new Registration();
        r.setNumRegistration(id);
        r.setSkier(skier);
        r.setCourse(slot == null ? course : slot.getCourse());
        r.setPlanningSlot(slot);
        r.setCancelled(cancelled);
        return r;
    }

    // ---------- registerSlot ----------

    @Test
    void registerSlotCreatesRegistrationForOpenSlot() {
        PlanningSlot slot = openSlot(7L);

        Registration result = service.registerSlot(1L, 7L);

        assertSame(skier, result.getSkier());
        assertSame(course, result.getCourse());
        assertSame(slot, result.getPlanningSlot());
        assertEquals(monday.atTime(10, 0).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), result.getNumWeek());
        assertEquals(SlotStatus.OPEN, slot.getStatus());
        verify(entityManager).refresh(slot);
    }

    @Test
    void registerSlotMarksSlotFullWhenLastSeatIsTaken() {
        PlanningSlot slot = slot(7L, monday, 10, 2, SlotStatus.OPEN);
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));
        when(registrationRepository.findByPlanningSlot_Id(7L))
                .thenReturn(Arrays.asList(registration(10L, slot, false), registration(11L, slot, true)));

        service.registerSlot(1L, 7L);

        assertEquals(SlotStatus.FULL, slot.getStatus());
    }

    @Test
    void registerSlotRejectsWhenCapacityReached() {
        PlanningSlot slot = slot(7L, monday, 10, 1, SlotStatus.OPEN);
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));
        when(registrationRepository.findByPlanningSlot_Id(7L))
                .thenReturn(Collections.singletonList(registration(10L, slot, false)));

        assertConflict(() -> service.registerSlot(1L, 7L));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void registerSlotRejectsFullCancelledOrStartedSlots() {
        PlanningSlot full = slot(7L, monday, 10, 4, SlotStatus.FULL);
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(full));
        assertConflict(() -> service.registerSlot(1L, 7L));

        PlanningSlot cancelled = slot(8L, monday, 10, 4, SlotStatus.CANCELLED);
        when(slotRepository.lockById(8L)).thenReturn(Optional.of(cancelled));
        assertConflict(() -> service.registerSlot(1L, 8L));

        PlanningSlot started = slot(9L, monday, 10, 4, SlotStatus.OPEN);
        started.setStartTime(LocalDateTime.now().minusHours(1));
        started.setEndTime(LocalDateTime.now().plusHours(1));
        when(slotRepository.lockById(9L)).thenReturn(Optional.of(started));
        assertConflict(() -> service.registerSlot(1L, 9L));
    }

    @Test
    void registerSlotRejectsInvalidIdentifiersAndMissingEntities() {
        assertBadRequest(() -> service.registerSlot(null, 7L));
        assertBadRequest(() -> service.registerSlot(1L, 0L));

        when(skierRepository.lockById(2L)).thenReturn(Optional.<Skier>empty());
        assertNotFound(() -> service.registerSlot(2L, 7L));

        when(slotRepository.lockById(404L)).thenReturn(Optional.<PlanningSlot>empty());
        assertNotFound(() -> service.registerSlot(1L, 404L));
    }

    @Test
    void registerSlotRequiresSkierDateOfBirth() {
        openSlot(7L);
        skier.setDateOfBirth(null);

        assertBadRequest(() -> service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotEnforcesAgeThresholdForChildrenCourses() {
        course.setTypeCourse(TypeCourse.COLLECTIVE_CHILDREN);
        openSlot(7L);

        skier.setDateOfBirth(monday.minusYears(16)); // turns 16 on the day of the lesson
        assertConflict(() -> service.registerSlot(1L, 7L));

        skier.setDateOfBirth(monday.minusYears(16).plusDays(1)); // still 15
        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotEnforcesAgeThresholdForAdultCourses() {
        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        openSlot(7L);

        skier.setDateOfBirth(monday.minusYears(16).plusDays(1)); // 15 years old
        assertConflict(() -> service.registerSlot(1L, 7L));

        skier.setDateOfBirth(monday.minusYears(16)); // exactly 16
        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotAcceptsAnyAgeForIndividualCourses() {
        course.setTypeCourse(TypeCourse.INDIVIDUAL);
        openSlot(7L);
        skier.setDateOfBirth(monday.minusYears(6));

        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotRejectsOverlappingRegistration() {
        openSlot(7L);
        PlanningSlot other = slot(20L, monday, 10, 4, SlotStatus.OPEN);
        other.setCourse(otherCourse());
        when(registrationRepository.findBySkier_NumSkier(1L))
                .thenReturn(Collections.singletonList(registration(30L, other, false)));

        assertConflict(() -> service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotRejectsSameCourseTwiceInTheSameIsoWeek() {
        openSlot(7L);
        PlanningSlot wednesday = slot(21L, monday.plusDays(2), 10, 4, SlotStatus.OPEN);
        when(registrationRepository.findBySkier_NumSkier(1L))
                .thenReturn(Collections.singletonList(registration(31L, wednesday, false)));

        assertConflict(() -> service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotAllowsSameCourseInAnotherWeek() {
        openSlot(7L);
        PlanningSlot nextWeek = slot(22L, monday.plusWeeks(1), 10, 4, SlotStatus.OPEN);
        when(registrationRepository.findBySkier_NumSkier(1L))
                .thenReturn(Collections.singletonList(registration(32L, nextWeek, false)));

        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotAllowsAnotherCourseInTheSameWeek() {
        openSlot(7L);
        PlanningSlot wednesday = slot(23L, monday.plusDays(2), 10, 4, SlotStatus.OPEN);
        wednesday.setCourse(otherCourse());
        when(registrationRepository.findBySkier_NumSkier(1L))
                .thenReturn(Collections.singletonList(registration(33L, wednesday, false)));

        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotIgnoresCancelledRegistrationsAndCancelledSlots() {
        openSlot(7L);
        PlanningSlot overlapping = slot(24L, monday, 10, 4, SlotStatus.OPEN);
        PlanningSlot cancelledSlot = slot(25L, monday, 10, 4, SlotStatus.CANCELLED);
        when(registrationRepository.findBySkier_NumSkier(1L)).thenReturn(Arrays.asList(
                registration(34L, overlapping, true),
                registration(35L, cancelledSlot, false)));

        assertNotNull(service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotRejectsHistoricalRegistrationOfSameCourseAndWeek() {
        PlanningSlot slot = openSlot(7L);
        Registration legacy = registration(36L, null, false);
        legacy.setNumWeek(slot.getStartTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        when(registrationRepository.findBySkier_NumSkier(1L)).thenReturn(Collections.singletonList(legacy));

        assertConflict(() -> service.registerSlot(1L, 7L));
    }

    @Test
    void registerSlotIgnoresHistoricalRegistrationOfAnotherWeekOrCourse() {
        PlanningSlot slot = openSlot(7L);
        int week = slot.getStartTime().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        Registration otherWeek = registration(37L, null, false);
        otherWeek.setNumWeek(week == 1 ? 2 : week - 1);
        Registration differentCourse = registration(38L, null, false);
        differentCourse.setCourse(otherCourse());
        differentCourse.setNumWeek(week);
        Registration noCourse = registration(39L, null, false);
        noCourse.setCourse(null);
        noCourse.setNumWeek(week);
        when(registrationRepository.findBySkier_NumSkier(1L)).thenReturn(Arrays.asList(otherWeek, differentCourse, noCourse));

        assertNotNull(service.registerSlot(1L, 7L));
    }

    private Course otherCourse() {
        Course c = new Course();
        c.setNumCourse(99L);
        c.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        return c;
    }

    // ---------- addRegistrationAndAssignToSkier ----------

    @Test
    void addRegistrationAndAssignToSkierDelegatesToRegisterSlot() {
        openSlot(7L);
        Registration input = new Registration();
        PlanningSlot ref = new PlanningSlot();
        ref.setId(7L);
        input.setPlanningSlot(ref);

        Registration result = service.addRegistrationAndAssignToSkier(input, 1L);

        assertSame(skier, result.getSkier());
    }

    @Test
    void addRegistrationAndAssignToSkierRejectsPresetIdOrMissingSlot() {
        Registration withId = new Registration();
        withId.setNumRegistration(5L);
        assertBadRequest(() -> service.addRegistrationAndAssignToSkier(withId, 1L));

        assertBadRequest(() -> service.addRegistrationAndAssignToSkier(new Registration(), 1L));
    }

    // ---------- assignRegistrationToCourse ----------

    @Test
    void assignRegistrationToCourseReturnsRegistrationAlreadyOnThatCourse() {
        Registration r = registration(10L, slot(7L, monday, 10, 4, SlotStatus.OPEN), false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));

        assertSame(r, service.assignRegistrationToCourse(10L, 3L));
    }

    @Test
    void assignRegistrationToCourseRefusesToMoveToAnotherCourse() {
        Registration r = registration(10L, slot(7L, monday, 10, 4, SlotStatus.OPEN), false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));
        when(courseRepository.findById(4L)).thenReturn(Optional.of(otherCourse()));

        assertConflict(() -> service.assignRegistrationToCourse(10L, 4L));
    }

    @Test
    void assignRegistrationToCourseRefusesRegistrationWithoutCourse() {
        Registration r = registration(10L, null, false);
        r.setCourse(null);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));

        assertConflict(() -> service.assignRegistrationToCourse(10L, 3L));
    }

    @Test
    void assignRegistrationToCourseRefusesCancelledRegistration() {
        Registration r = registration(10L, null, true);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));

        assertConflict(() -> service.assignRegistrationToCourse(10L, 3L));
    }

    @Test
    void assignRegistrationToCourseFailsOnMissingEntitiesOrInvalidIds() {
        when(registrationRepository.findById(10L)).thenReturn(Optional.<Registration>empty());
        assertNotFound(() -> service.assignRegistrationToCourse(10L, 3L));

        when(registrationRepository.findById(11L)).thenReturn(Optional.of(registration(11L, null, false)));
        when(courseRepository.findById(404L)).thenReturn(Optional.<Course>empty());
        assertNotFound(() -> service.assignRegistrationToCourse(11L, 404L));

        assertBadRequest(() -> service.assignRegistrationToCourse(null, 3L));
        assertBadRequest(() -> service.assignRegistrationToCourse(10L, 0L));
    }

    // ---------- addRegistrationAndAssignToSkierAndCourse ----------

    @Test
    void addRegistrationAndAssignToSkierAndCourseRegistersOnMatchingSlot() {
        openSlot(7L);
        Registration input = new Registration();
        PlanningSlot ref = new PlanningSlot();
        ref.setId(7L);
        input.setPlanningSlot(ref);

        Registration result = service.addRegistrationAndAssignToSkierAndCourse(input, 1L, 3L);

        assertSame(course, result.getCourse());
    }

    @Test
    void addRegistrationAndAssignToSkierAndCourseRejectsSlotOfAnotherCourse() {
        PlanningSlot slot = openSlot(7L);
        slot.setCourse(otherCourse());
        Registration input = new Registration();
        PlanningSlot ref = new PlanningSlot();
        ref.setId(7L);
        input.setPlanningSlot(ref);

        assertConflict(() -> service.addRegistrationAndAssignToSkierAndCourse(input, 1L, 3L));
    }

    @Test
    void addRegistrationAndAssignToSkierAndCourseValidatesInput() {
        assertBadRequest(() -> service.addRegistrationAndAssignToSkierAndCourse(new Registration(), 1L, null));

        when(courseRepository.findById(404L)).thenReturn(Optional.<Course>empty());
        assertNotFound(() -> service.addRegistrationAndAssignToSkierAndCourse(new Registration(), 1L, 404L));

        Registration withId = new Registration();
        withId.setNumRegistration(5L);
        assertBadRequest(() -> service.addRegistrationAndAssignToSkierAndCourse(withId, 1L, 3L));

        assertBadRequest(() -> service.addRegistrationAndAssignToSkierAndCourse(new Registration(), 1L, 3L));

        Registration slotWithoutId = new Registration();
        slotWithoutId.setPlanningSlot(new PlanningSlot());
        assertBadRequest(() -> service.addRegistrationAndAssignToSkierAndCourse(slotWithoutId, 1L, 3L));

        Registration unknownSlot = new Registration();
        PlanningSlot ref = new PlanningSlot();
        ref.setId(404L);
        unknownSlot.setPlanningSlot(ref);
        when(slotRepository.findById(404L)).thenReturn(Optional.<PlanningSlot>empty());
        assertNotFound(() -> service.addRegistrationAndAssignToSkierAndCourse(unknownSlot, 1L, 3L));
    }

    // ---------- numWeeksCourseOfInstructorBySupport ----------

    @Test
    void numWeeksReturnsRepositoryResultForKnownInstructor() {
        when(instructorRepository.findById(2L)).thenReturn(Optional.of(new Instructor()));
        when(registrationRepository.numWeeksCourseOfInstructorBySupport(2L, Support.SKI)).thenReturn(Arrays.asList(3, 4));

        List<Integer> result = service.numWeeksCourseOfInstructorBySupport(2L, Support.SKI);

        assertEquals(Arrays.asList(3, 4), result);
    }

    @Test
    void numWeeksValidatesArguments() {
        assertBadRequest(() -> service.numWeeksCourseOfInstructorBySupport(null, Support.SKI));
        assertBadRequest(() -> service.numWeeksCourseOfInstructorBySupport(2L, null));

        when(instructorRepository.findById(2L)).thenReturn(Optional.<Instructor>empty());
        assertNotFound(() -> service.numWeeksCourseOfInstructorBySupport(2L, Support.SKI));
    }

    // ---------- nextAvailableSlot ----------

    @Test
    void nextAvailableSlotPicksEarliestOpenFutureSlotOfTheCourse() {
        PlanningSlot later = slot(1L, monday.plusDays(2), 10, 4, SlotStatus.OPEN);
        PlanningSlot earlier = slot(2L, monday, 10, 4, SlotStatus.OPEN);
        PlanningSlot full = slot(3L, monday.minusDays(1), 10, 4, SlotStatus.FULL);
        PlanningSlot cancelled = slot(4L, monday.minusDays(1), 11, 4, SlotStatus.CANCELLED);
        PlanningSlot wrongCourse = slot(5L, monday.minusDays(1), 12, 4, SlotStatus.OPEN);
        wrongCourse.setCourse(otherCourse());
        PlanningSlot past = slot(6L, LocalDate.now().minusDays(3), 10, 4, SlotStatus.OPEN);
        when(slotRepository.findAll()).thenReturn(Arrays.asList(later, earlier, full, cancelled, wrongCourse, past));

        assertSame(earlier, service.nextAvailableSlot(3L));
    }

    @Test
    void nextAvailableSlotFailsWhenNothingIsAvailable() {
        when(slotRepository.findAll()).thenReturn(Collections.<PlanningSlot>emptyList());

        assertConflict(() -> service.nextAvailableSlot(3L));
    }

    // ---------- cancel ----------

    @Test
    void cancelMarksRegistrationCancelledAndReopensFullSlot() {
        PlanningSlot slot = slot(7L, monday, 10, 1, SlotStatus.FULL);
        Registration r = registration(10L, slot, false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));

        service.cancel(10L);

        assertTrue(r.isCancelled());
        assertEquals(SlotStatus.OPEN, slot.getStatus());
        verify(entityManager).refresh(r);
        verify(entityManager).refresh(slot);
    }

    @Test
    void cancelKeepsOpenSlotOpen() {
        PlanningSlot slot = slot(7L, monday, 10, 4, SlotStatus.OPEN);
        Registration r = registration(10L, slot, false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));

        service.cancel(10L);

        assertTrue(r.isCancelled());
        assertEquals(SlotStatus.OPEN, slot.getStatus());
    }

    @Test
    void cancelSupportsHistoricalRegistrationWithoutSlot() {
        Registration r = registration(10L, null, false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));

        service.cancel(10L);

        assertTrue(r.isCancelled());
        verify(slotRepository, never()).lockById(anyLong());
    }

    @Test
    void cancelIsIdempotentForAlreadyCancelledRegistration() {
        PlanningSlot slot = slot(7L, monday, 10, 1, SlotStatus.FULL);
        Registration r = registration(10L, slot, true);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));

        service.cancel(10L);

        assertEquals(SlotStatus.FULL, slot.getStatus());
    }

    @Test
    void cancelRefusesWhenLessonAlreadyStarted() {
        PlanningSlot slot = slot(7L, monday, 10, 4, SlotStatus.OPEN);
        slot.setStartTime(LocalDateTime.now().minusHours(1));
        slot.setEndTime(LocalDateTime.now().plusHours(1));
        Registration r = registration(10L, slot, false);
        when(registrationRepository.findById(10L)).thenReturn(Optional.of(r));
        when(slotRepository.lockById(7L)).thenReturn(Optional.of(slot));

        assertConflict(() -> service.cancel(10L));
        assertFalse(r.isCancelled());
    }

    @Test
    void cancelFailsOnInvalidOrMissingEntities() {
        assertBadRequest(() -> service.cancel(null));

        when(registrationRepository.findById(10L)).thenReturn(Optional.<Registration>empty());
        assertNotFound(() -> service.cancel(10L));

        Registration orphanSkier = registration(11L, null, false);
        orphanSkier.getSkier().setNumSkier(2L);
        when(registrationRepository.findById(11L)).thenReturn(Optional.of(orphanSkier));
        when(skierRepository.lockById(2L)).thenReturn(Optional.<Skier>empty());
        assertNotFound(() -> service.cancel(11L));

        skier.setNumSkier(1L); // restore the identifier mutated above so only the slot lookup can fail
        PlanningSlot slot = slot(8L, monday, 10, 4, SlotStatus.OPEN);
        Registration orphanSlot = registration(12L, slot, false);
        when(registrationRepository.findById(12L)).thenReturn(Optional.of(orphanSlot));
        when(slotRepository.lockById(8L)).thenReturn(Optional.<PlanningSlot>empty());
        assertNotFound(() -> service.cancel(12L));
    }

    // ---------- bySkier / search ----------

    @Test
    void bySkierReturnsHistory() {
        Registration r = registration(10L, null, false);
        when(registrationRepository.findBySkier_NumSkier(1L)).thenReturn(Collections.singletonList(r));

        assertEquals(1, service.bySkier(1L).size());
    }

    @Test
    void bySkierFailsWhenSkierMissingOrIdInvalid() {
        when(skierRepository.findById(2L)).thenReturn(Optional.<Skier>empty());

        assertNotFound(() -> service.bySkier(2L));
        assertBadRequest(() -> service.bySkier(0L));
    }

    @Test
    void searchValidatesPeriodAndCourse() {
        LocalDateTime now = LocalDateTime.now();

        assertBadRequest(() -> service.search(null, now, null));
        assertBadRequest(() -> service.search(now, null, null));
        assertBadRequest(() -> service.search(now, now, null));
        assertBadRequest(() -> service.search(now, now.plusDays(1), 0L));

        when(courseRepository.findById(404L)).thenReturn(Optional.<Course>empty());
        assertNotFound(() -> service.search(now, now.plusDays(1), 404L));
    }

    @Test
    void searchReturnsOnlyDatedRegistrationsOverlappingThePeriod() {
        PlanningSlot inside = slot(1L, monday, 10, 4, SlotStatus.OPEN);
        PlanningSlot outside = slot(2L, monday.plusDays(10), 10, 4, SlotStatus.OPEN);
        Registration in = registration(10L, inside, false);
        Registration out = registration(11L, outside, false);
        Registration legacy = registration(12L, null, false);
        Registration otherCourseReg = registration(13L, slot(3L, monday, 11, 4, SlotStatus.OPEN), false);
        otherCourseReg.setCourse(otherCourse());
        when(registrationRepository.findAll()).thenReturn(Arrays.asList(in, out, legacy, otherCourseReg));

        List<Registration> all = service.search(monday.atStartOfDay(), monday.plusDays(1).atStartOfDay(), null);
        List<Registration> filtered = service.search(monday.atStartOfDay(), monday.plusDays(1).atStartOfDay(), 3L);

        assertEquals(2, all.size());
        assertTrue(all.contains(in));
        assertTrue(all.contains(otherCourseReg));
        assertEquals(1, filtered.size());
        assertSame(in, filtered.get(0));
    }
}

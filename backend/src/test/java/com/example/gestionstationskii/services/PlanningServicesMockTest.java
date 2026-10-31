package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.example.gestionstationskii.services.BusinessAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanningServicesMockTest {

    @Mock private PlanningSlotRepository slots;
    @Mock private InstructorAvailabilityRepository availabilities;
    @Mock private IInstructorRepository instructors;
    @Mock private ICourseRepository courses;
    @Mock private IRegistrationRepository registrations;

    @InjectMocks
    private PlanningServicesImpl service;

    private LocalDate day;
    private Course course;
    private Instructor instructor;

    @BeforeEach
    void setUp() {
        day = LocalDate.now().plusDays(14);
        course = new Course();
        course.setNumCourse(3L);
        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        instructor = new Instructor();
        instructor.setNumInstructor(2L);
        instructor.setCourses(new HashSet<>(Collections.singleton(course)));

        when(courses.lockById(3L)).thenReturn(Optional.of(course));
        when(courses.findById(3L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
        when(instructors.findById(2L)).thenReturn(Optional.of(instructor));
        when(availabilities.findByInstructor_NumInstructor(2L))
                .thenReturn(Collections.singletonList(availability(day.getDayOfWeek(), 8, 18)));
        when(slots.findAll()).thenReturn(Collections.<PlanningSlot>emptyList());
        when(slots.save(any(PlanningSlot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private InstructorAvailability availability(DayOfWeek dow, int fromHour, int toHour) {
        InstructorAvailability a = new InstructorAvailability();
        a.setInstructor(instructor);
        a.setDayOfWeek(dow);
        a.setStartTime(LocalTime.of(fromHour, 0));
        a.setEndTime(LocalTime.of(toHour, 0));
        a.setValidFrom(LocalDate.now());
        a.setValidUntil(LocalDate.now().plusDays(60));
        return a;
    }

    private Requests.Slot slotInput(int startHour, int endHour, Integer capacity) {
        Requests.Slot in = new Requests.Slot();
        in.setCourseId(3L);
        in.setInstructorId(2L);
        in.setStartTime(day.atTime(startHour, 0));
        in.setEndTime(day.atTime(endHour, 0));
        in.setMaxCapacity(capacity);
        return in;
    }

    private PlanningSlot existingSlot(Long id, int startHour, int endHour, SlotStatus status) {
        PlanningSlot s = new PlanningSlot();
        s.setId(id);
        s.setCourse(course);
        s.setInstructor(instructor);
        s.setStartTime(day.atTime(startHour, 0));
        s.setEndTime(day.atTime(endHour, 0));
        s.setMaxCapacity(4);
        s.setStatus(status);
        return s;
    }

    private Requests.Availability availabilityInput() {
        Requests.Availability in = new Requests.Availability();
        in.setDayOfWeek(DayOfWeek.MONDAY);
        in.setStartTime(LocalTime.of(8, 0));
        in.setEndTime(LocalTime.of(12, 0));
        in.setValidFrom(LocalDate.now());
        in.setValidUntil(LocalDate.now().plusDays(30));
        return in;
    }

    // ---------- addAvailability ----------

    @Test
    void addAvailabilityPersistsValidInput() {
        when(availabilities.save(any(InstructorAvailability.class))).thenAnswer(inv -> inv.getArgument(0));

        InstructorAvailability saved = service.addAvailability(2L, availabilityInput());

        assertSame(instructor, saved.getInstructor());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), saved.getStartTime());
        assertEquals(LocalTime.of(12, 0), saved.getEndTime());
        verify(availabilities).save(any(InstructorAvailability.class));
    }

    @Test
    void addAvailabilityRejectsInvalidInstructorId() {
        assertBadRequest(() -> service.addAvailability(null, availabilityInput()));
        assertBadRequest(() -> service.addAvailability(0L, availabilityInput()));
    }

    @Test
    void addAvailabilityFailsWhenInstructorMissing() {
        when(instructors.lockById(77L)).thenReturn(Optional.<Instructor>empty());

        assertNotFound(() -> service.addAvailability(77L, availabilityInput()));
    }

    @Test
    void addAvailabilityRejectsInvalidTimes() {
        Requests.Availability noDay = availabilityInput();
        noDay.setDayOfWeek(null);
        assertBadRequest(() -> service.addAvailability(2L, noDay));

        Requests.Availability noStart = availabilityInput();
        noStart.setStartTime(null);
        assertBadRequest(() -> service.addAvailability(2L, noStart));

        Requests.Availability noEnd = availabilityInput();
        noEnd.setEndTime(null);
        assertBadRequest(() -> service.addAvailability(2L, noEnd));

        Requests.Availability reversed = availabilityInput();
        reversed.setStartTime(LocalTime.of(14, 0));
        reversed.setEndTime(LocalTime.of(9, 0));
        assertBadRequest(() -> service.addAvailability(2L, reversed));

        Requests.Availability empty = availabilityInput();
        empty.setEndTime(empty.getStartTime());
        assertBadRequest(() -> service.addAvailability(2L, empty));

        verify(availabilities, never()).save(any(InstructorAvailability.class));
    }

    @Test
    void addAvailabilityRejectsInvalidValidityPeriod() {
        Requests.Availability noFrom = availabilityInput();
        noFrom.setValidFrom(null);
        assertBadRequest(() -> service.addAvailability(2L, noFrom));

        Requests.Availability noUntil = availabilityInput();
        noUntil.setValidUntil(null);
        assertBadRequest(() -> service.addAvailability(2L, noUntil));

        Requests.Availability inverted = availabilityInput();
        inverted.setValidFrom(LocalDate.now().plusDays(10));
        inverted.setValidUntil(LocalDate.now());
        assertBadRequest(() -> service.addAvailability(2L, inverted));
    }

    // ---------- availability ----------

    @Test
    void availabilityReturnsInstructorAvailabilities() {
        List<InstructorAvailability> result = service.availability(2L);

        assertEquals(1, result.size());
    }

    @Test
    void availabilityFailsWhenInstructorMissing() {
        when(instructors.findById(55L)).thenReturn(Optional.<Instructor>empty());

        assertNotFound(() -> service.availability(55L));
    }

    @Test
    void availabilityRejectsInvalidId() {
        assertBadRequest(() -> service.availability(-1L));
    }

    // ---------- get ----------

    @Test
    void getReturnsSlot() {
        PlanningSlot slot = existingSlot(9L, 10, 11, SlotStatus.OPEN);
        when(slots.findById(9L)).thenReturn(Optional.of(slot));

        assertSame(slot, service.get(9L));
    }

    @Test
    void getFailsWhenMissingOrInvalid() {
        when(slots.findById(9L)).thenReturn(Optional.<PlanningSlot>empty());

        assertNotFound(() -> service.get(9L));
        assertBadRequest(() -> service.get(null));
    }

    // ---------- create ----------

    @Test
    void createBuildsOpenSlot() {
        PlanningSlot created = service.create(slotInput(10, 11, 4));

        assertEquals(SlotStatus.OPEN, created.getStatus());
        assertSame(course, created.getCourse());
        assertSame(instructor, created.getInstructor());
        assertEquals(4, created.getMaxCapacity());
        assertEquals(day.atTime(10, 0), created.getStartTime());
        assertEquals(day.atTime(11, 0), created.getEndTime());
    }

    @Test
    void createRejectsInvalidIdentifiers() {
        Requests.Slot noCourse = slotInput(10, 11, 4);
        noCourse.setCourseId(null);
        assertBadRequest(() -> service.create(noCourse));

        Requests.Slot noInstructor = slotInput(10, 11, 4);
        noInstructor.setInstructorId(0L);
        assertBadRequest(() -> service.create(noInstructor));
    }

    @Test
    void createRejectsInvalidTimeWindows() {
        Requests.Slot noStart = slotInput(10, 11, 4);
        noStart.setStartTime(null);
        assertBadRequest(() -> service.create(noStart));

        Requests.Slot noEnd = slotInput(10, 11, 4);
        noEnd.setEndTime(null);
        assertBadRequest(() -> service.create(noEnd));

        assertBadRequest(() -> service.create(slotInput(11, 10, 4)));

        Requests.Slot past = slotInput(10, 11, 4);
        past.setStartTime(LocalDateTime.now().minusDays(1));
        past.setEndTime(LocalDateTime.now().minusDays(1).plusHours(1));
        assertBadRequest(() -> service.create(past));

        Requests.Slot overMidnight = slotInput(22, 23, 4);
        overMidnight.setEndTime(day.plusDays(1).atTime(1, 0));
        assertBadRequest(() -> service.create(overMidnight));

        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void createFailsWhenCourseOrInstructorMissing() {
        when(courses.lockById(3L)).thenReturn(Optional.<Course>empty());
        assertNotFound(() -> service.create(slotInput(10, 11, 4)));

        when(courses.lockById(3L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.<Instructor>empty());
        assertNotFound(() -> service.create(slotInput(10, 11, 4)));
    }

    @Test
    void createRejectsInstructorNotLinkedToCourse() {
        instructor.setCourses(new HashSet<Course>());

        assertConflict(() -> service.create(slotInput(10, 11, 4)));
    }

    @Test
    void createEnforcesCapacityCeilings() {
        assertBadRequest(() -> service.create(slotInput(10, 11, null)));
        assertBadRequest(() -> service.create(slotInput(10, 11, 0)));
        assertBadRequest(() -> service.create(slotInput(10, 11, 7)));

        course.setTypeCourse(TypeCourse.INDIVIDUAL);
        assertBadRequest(() -> service.create(slotInput(10, 11, 2)));
        assertEquals(1, service.create(slotInput(10, 11, 1)).getMaxCapacity());

        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        assertEquals(6, service.create(slotInput(10, 11, 6)).getMaxCapacity());
    }

    @Test
    void createRejectsSlotOutsideInstructorAvailability() {
        // Availability covers 08:00-18:00 on the right day.
        assertConflict(() -> service.create(slotInput(6, 7, 4)));
        assertConflict(() -> service.create(slotInput(17, 19, 4)));

        // Availability on another weekday only.
        DayOfWeek other = day.getDayOfWeek().plus(1);
        when(availabilities.findByInstructor_NumInstructor(2L))
                .thenReturn(Collections.singletonList(availability(other, 8, 18)));
        assertConflict(() -> service.create(slotInput(10, 11, 4)));

        // Availability expired before the slot date.
        InstructorAvailability expired = availability(day.getDayOfWeek(), 8, 18);
        expired.setValidUntil(day.minusDays(1));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(expired));
        assertConflict(() -> service.create(slotInput(10, 11, 4)));

        // Availability starting after the slot date.
        InstructorAvailability notYet = availability(day.getDayOfWeek(), 8, 18);
        notYet.setValidFrom(day.plusDays(1));
        notYet.setValidUntil(day.plusDays(30));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(notYet));
        assertConflict(() -> service.create(slotInput(10, 11, 4)));
    }

    @Test
    void createRejectsOverlapWithAnotherSlotOfTheInstructor() {
        when(slots.findAll()).thenReturn(Collections.singletonList(existingSlot(50L, 10, 12, SlotStatus.OPEN)));

        assertConflict(() -> service.create(slotInput(11, 13, 4)));
    }

    @Test
    void createIgnoresCancelledAndAdjacentSlots() {
        when(slots.findAll()).thenReturn(Arrays.asList(
                existingSlot(50L, 10, 12, SlotStatus.CANCELLED),
                existingSlot(51L, 8, 10, SlotStatus.OPEN)));

        PlanningSlot created = service.create(slotInput(10, 11, 4));

        assertEquals(SlotStatus.OPEN, created.getStatus());
    }

    @Test
    void createIgnoresOverlapsOfOtherInstructors() {
        Instructor other = new Instructor();
        other.setNumInstructor(99L);
        PlanningSlot theirs = existingSlot(50L, 10, 12, SlotStatus.OPEN);
        theirs.setInstructor(other);
        when(slots.findAll()).thenReturn(Collections.singletonList(theirs));

        assertNotNull(service.create(slotInput(10, 11, 4)));
    }

    // ---------- update ----------

    @Test
    void updateModifiesFutureSlotWithoutRegistrations() {
        PlanningSlot current = existingSlot(50L, 10, 11, SlotStatus.OPEN);
        when(slots.lockById(50L)).thenReturn(Optional.of(current));
        when(slots.findAll()).thenReturn(Collections.singletonList(current));
        when(registrations.findByPlanningSlot_Id(50L)).thenReturn(Collections.<Registration>emptyList());

        PlanningSlot updated = service.update(50L, slotInput(14, 15, 5));

        assertSame(current, updated);
        assertEquals(day.atTime(14, 0), updated.getStartTime());
        assertEquals(5, updated.getMaxCapacity());
        assertEquals(SlotStatus.OPEN, updated.getStatus());
    }

    @Test
    void updateFailsWhenSlotMissingOrIdInvalid() {
        when(slots.lockById(50L)).thenReturn(Optional.<PlanningSlot>empty());

        assertNotFound(() -> service.update(50L, slotInput(10, 11, 4)));
        assertBadRequest(() -> service.update(null, slotInput(10, 11, 4)));
    }

    @Test
    void updateRejectsCancelledSlot() {
        when(slots.lockById(50L)).thenReturn(Optional.of(existingSlot(50L, 10, 11, SlotStatus.CANCELLED)));

        assertConflict(() -> service.update(50L, slotInput(10, 11, 4)));
    }

    @Test
    void updateRejectsAlreadyStartedSlot() {
        PlanningSlot started = existingSlot(50L, 10, 11, SlotStatus.OPEN);
        started.setStartTime(LocalDateTime.now().minusHours(1));
        started.setEndTime(LocalDateTime.now().plusHours(1));
        when(slots.lockById(50L)).thenReturn(Optional.of(started));

        assertConflict(() -> service.update(50L, slotInput(10, 11, 4)));
    }

    @Test
    void updateRejectsSlotWithRegistrationHistory() {
        when(slots.lockById(50L)).thenReturn(Optional.of(existingSlot(50L, 10, 11, SlotStatus.OPEN)));
        when(registrations.findByPlanningSlot_Id(50L)).thenReturn(Collections.singletonList(new Registration()));

        assertConflict(() -> service.update(50L, slotInput(10, 11, 4)));
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    // ---------- cancel ----------

    @Test
    void cancelMarksSlotAndItsRegistrationsCancelled() {
        PlanningSlot slot = existingSlot(50L, 10, 11, SlotStatus.OPEN);
        Registration r1 = new Registration();
        Registration r2 = new Registration();
        when(slots.lockById(50L)).thenReturn(Optional.of(slot));
        when(registrations.findByPlanningSlot_Id(50L)).thenReturn(Arrays.asList(r1, r2));

        service.cancel(50L);

        assertEquals(SlotStatus.CANCELLED, slot.getStatus());
        assertTrue(r1.isCancelled());
        assertTrue(r2.isCancelled());
    }

    @Test
    void cancelRejectsStartedSlot() {
        PlanningSlot started = existingSlot(50L, 10, 11, SlotStatus.OPEN);
        started.setStartTime(LocalDateTime.now().minusMinutes(5));
        started.setEndTime(LocalDateTime.now().plusMinutes(55));
        when(slots.lockById(50L)).thenReturn(Optional.of(started));

        assertConflict(() -> service.cancel(50L));
        assertEquals(SlotStatus.OPEN, started.getStatus());
    }

    @Test
    void cancelFailsWhenMissingOrInvalid() {
        when(slots.lockById(50L)).thenReturn(Optional.<PlanningSlot>empty());

        assertNotFound(() -> service.cancel(50L));
        assertBadRequest(() -> service.cancel(0L));
    }

    // ---------- search ----------

    @Test
    void searchRejectsInvalidPeriod() {
        LocalDateTime now = LocalDateTime.now();

        assertBadRequest(() -> service.search(null, now, null, null, false));
        assertBadRequest(() -> service.search(now, null, null, null, false));
        assertBadRequest(() -> service.search(now, now, null, null, false));
        assertBadRequest(() -> service.search(now.plusHours(1), now, null, null, false));
    }

    @Test
    void searchFailsWhenFilterEntitiesAreMissing() {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        when(courses.findById(404L)).thenReturn(Optional.<Course>empty());
        when(instructors.findById(405L)).thenReturn(Optional.<Instructor>empty());

        assertNotFound(() -> service.search(from, to, 404L, null, false));
        assertNotFound(() -> service.search(from, to, null, 405L, false));
        assertBadRequest(() -> service.search(from, to, 0L, null, false));
        assertBadRequest(() -> service.search(from, to, null, -2L, false));
    }

    @Test
    void searchReturnsOverlappingSlotsSortedByStart() {
        PlanningSlot late = existingSlot(1L, 15, 16, SlotStatus.OPEN);
        PlanningSlot early = existingSlot(2L, 9, 10, SlotStatus.OPEN);
        PlanningSlot outside = existingSlot(3L, 9, 10, SlotStatus.OPEN);
        outside.setStartTime(day.plusDays(5).atTime(9, 0));
        outside.setEndTime(day.plusDays(5).atTime(10, 0));
        when(slots.findAll()).thenReturn(Arrays.asList(late, early, outside));

        List<PlanningSlot> result = service.search(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), null, null, false);

        assertEquals(2, result.size());
        assertSame(early, result.get(0));
        assertSame(late, result.get(1));
    }

    @Test
    void searchAppliesCourseAndInstructorFilters() {
        Course otherCourse = new Course();
        otherCourse.setNumCourse(8L);
        Instructor otherInstructor = new Instructor();
        otherInstructor.setNumInstructor(9L);
        PlanningSlot match = existingSlot(1L, 9, 10, SlotStatus.OPEN);
        PlanningSlot wrongCourse = existingSlot(2L, 10, 11, SlotStatus.OPEN);
        wrongCourse.setCourse(otherCourse);
        PlanningSlot wrongInstructor = existingSlot(3L, 11, 12, SlotStatus.OPEN);
        wrongInstructor.setInstructor(otherInstructor);
        when(slots.findAll()).thenReturn(Arrays.asList(match, wrongCourse, wrongInstructor));

        List<PlanningSlot> result = service.search(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), 3L, 2L, false);

        assertEquals(1, result.size());
        assertSame(match, result.get(0));
    }

    @Test
    void searchAvailableOnlyKeepsOpenFutureSlots() {
        PlanningSlot open = existingSlot(1L, 9, 10, SlotStatus.OPEN);
        PlanningSlot full = existingSlot(2L, 10, 11, SlotStatus.FULL);
        PlanningSlot cancelled = existingSlot(3L, 11, 12, SlotStatus.CANCELLED);
        PlanningSlot past = existingSlot(4L, 12, 13, SlotStatus.OPEN);
        past.setStartTime(LocalDateTime.now().minusHours(2));
        past.setEndTime(LocalDateTime.now().minusHours(1));
        when(slots.findAll()).thenReturn(Arrays.asList(open, full, cancelled, past));

        List<PlanningSlot> result = service.search(LocalDateTime.now().minusDays(1), day.plusDays(1).atStartOfDay(), null, null, true);

        assertEquals(1, result.size());
        assertSame(open, result.get(0));
    }
}

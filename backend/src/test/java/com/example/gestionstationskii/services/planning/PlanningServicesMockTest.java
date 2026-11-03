package com.example.gestionstationskii.services.planning;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IPlanningServices;
import com.example.gestionstationskii.services.PlanningServicesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class PlanningServicesMockTest {
    private PlanningSlotRepository slots;
    private InstructorAvailabilityRepository availabilities;
    private IInstructorRepository instructors;
    private ICourseRepository courses;
    private IRegistrationRepository registrations;
    private IPlanningServices service;

    @BeforeEach
    void setUp() {
        slots = mock(PlanningSlotRepository.class);
        availabilities = mock(InstructorAvailabilityRepository.class);
        instructors = mock(IInstructorRepository.class);
        courses = mock(ICourseRepository.class);
        registrations = mock(IRegistrationRepository.class);
        service = new PlanningServicesImpl(slots, availabilities, instructors, courses, registrations);
    }

    @Test
    void addAvailabilityPersistsTheInstructorSchedule() {
        Instructor instructor = instructor(2L, course(1L));
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
        when(availabilities.save(any(InstructorAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Requests.Availability input = availabilityInput();

        InstructorAvailability saved = service.addAvailability(2L, input);

        assertSame(instructor, saved.getInstructor());
        assertEquals(DayOfWeek.MONDAY, saved.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), saved.getStartTime());
        verify(availabilities).save(any(InstructorAvailability.class));
    }

    @Test
void addAvailabilityRejectsInvalidTimeRange() {
    when(instructors.lockById(2L)).thenReturn(Optional.of(instructor(2L)));
    Requests.Availability input = availabilityInput();
    input.setStartTime(LocalTime.of(12, 0));
    input.setEndTime(LocalTime.of(9, 0));

    assertThrows(BusinessException.class, () -> service.addAvailability(2L, input));
    verify(availabilities, never()).save(any(InstructorAvailability.class));
}

    @Test
    void availabilityRejectsUnknownInstructor() {
        when(instructors.existsById(99L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.availability(99L));

        verify(availabilities, never()).findByInstructor_NumInstructor(anyLong());
    }

    @Test
    void getReturnsSlotAndRejectsMissingSlot() {
        PlanningSlot slot = slot(8L, LocalDateTime.now().plusDays(2));
        when(slots.findById(8L)).thenReturn(Optional.of(slot));
        when(slots.findById(99L)).thenReturn(Optional.empty());

        assertSame(slot, service.get(8L));
        assertThrows(BusinessException.class, () -> service.get(99L));
    }

    @Test
    void createPersistsAValidSlotWithinInstructorAvailability() {
        Requests.Slot input = slotInput(1L, 2L, 6);
        stubValidCreate(input);
        when(slots.save(any(PlanningSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlanningSlot saved = service.create(input);

        assertEquals(SlotStatus.OPEN, saved.getStatus());
        assertEquals(6, saved.getMaxCapacity());
        assertEquals(1L, saved.getCourse().getNumCourse());
        assertEquals(2L, saved.getInstructor().getNumInstructor());
        verify(slots).save(any(PlanningSlot.class));
    }

    @Test
    void createRejectsCapacityAboveCollectiveLimit() {
        Requests.Slot input = slotInput(1L, 2L, 7);
        stubValidCreate(input);

        assertThrows(BusinessException.class, () -> service.create(input));

        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void createRejectsInstructorNotAssignedToCourse() {
        Requests.Slot input = slotInput(1L, 2L, 6);
        Course course = course(1L);
        Instructor instructor = instructor(2L);
        when(courses.lockById(1L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));

        assertThrows(BusinessException.class, () -> service.create(input));

        verify(availabilities, never()).findByInstructor_NumInstructor(anyLong());
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void updateRejectsSlotWithRegistrationHistory() {
        PlanningSlot current = slot(8L, LocalDateTime.now().plusDays(2));
        Requests.Slot update = slotInput(1L, 2L, 6);
        when(slots.lockById(8L)).thenReturn(Optional.of(current));
        when(registrations.findByPlanningSlot_Id(8L)).thenReturn(Collections.singletonList(new Registration()));

        assertThrows(BusinessException.class, () -> service.update(8L, update));

        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void updateRejectsCancelledOrStartedSlot() {
        PlanningSlot cancelled = slot(8L, LocalDateTime.now().plusDays(2));
        cancelled.setStatus(SlotStatus.CANCELLED);
        PlanningSlot started = slot(9L, LocalDateTime.now().minusHours(1));
        when(slots.lockById(8L)).thenReturn(Optional.of(cancelled));
        when(slots.lockById(9L)).thenReturn(Optional.of(started));
        Requests.Slot updateCancelled = slotInput(1L, 2L, 6);
        Requests.Slot updateStarted = slotInput(1L, 2L, 6);

        assertThrows(BusinessException.class, () -> service.update(8L, updateCancelled));
        assertThrows(BusinessException.class, () -> service.update(9L, updateStarted));
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void cancelMarksSlotAndItsRegistrationsAsCancelled() {
        PlanningSlot slot = slot(8L, LocalDateTime.now().plusDays(2));
        Registration registration = new Registration();
        when(slots.lockById(8L)).thenReturn(Optional.of(slot));
        when(registrations.findByPlanningSlot_Id(8L)).thenReturn(Collections.singletonList(registration));

        service.cancel(8L);

        assertEquals(SlotStatus.CANCELLED, slot.getStatus());
        assertTrue(registration.isCancelled());
    }

    @Test
    void searchFiltersAndSortsSlotsByStartTime() {
        LocalDateTime from = LocalDateTime.now().plusDays(1);
        Course selectedCourse = course(1L);
        Instructor selectedInstructor = instructor(2L, selectedCourse);
        PlanningSlot later = slot(2L, from.plusHours(2));
        later.setCourse(selectedCourse);
        later.setInstructor(selectedInstructor);
        PlanningSlot earlier = slot(1L, from.plusHours(1));
        earlier.setCourse(selectedCourse);
        earlier.setInstructor(selectedInstructor);
        PlanningSlot differentInstructor = slot(3L, from.plusHours(3));
        differentInstructor.setCourse(selectedCourse);
        differentInstructor.setInstructor(instructor(99L, selectedCourse));
        when(slots.findAll()).thenReturn(Arrays.asList(later, earlier, differentInstructor));
        when(courses.existsById(1L)).thenReturn(true);
        when(instructors.existsById(2L)).thenReturn(true);

        List<PlanningSlot> result = service.search(from, from.plusDays(1), 1L, 2L, false);

        assertEquals(Arrays.asList(earlier, later), result);
    }

    @Test
    void availabilityValidatesInstructorAndAllTimeAndDateFields() {
        Requests.Availability valid = availabilityInput();
        when(instructors.lockById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, valid));

        Instructor instructor = instructor(2L);
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
        Requests.Availability missingDay = availabilityInput();
        missingDay.setDayOfWeek(null);
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, missingDay));
        Requests.Availability missingStart = availabilityInput();
        missingStart.setStartTime(null);
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, missingStart));
        Requests.Availability missingEnd = availabilityInput();
        missingEnd.setEndTime(null);
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, missingEnd));
        Requests.Availability missingValidFrom = availabilityInput();
        missingValidFrom.setValidFrom(null);
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, missingValidFrom));
        Requests.Availability missingValidUntil = availabilityInput();
        missingValidUntil.setValidUntil(null);
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, missingValidUntil));
        Requests.Availability reversedPeriod = availabilityInput();
        reversedPeriod.setValidFrom(LocalDate.now().plusDays(5));
        reversedPeriod.setValidUntil(LocalDate.now().plusDays(1));
        assertThrows(BusinessException.class, () -> service.addAvailability(2L, reversedPeriod));
        verify(availabilities, never()).save(any(InstructorAvailability.class));
    }

    @Test
    void createRejectsPastCrossDayAndInvalidCapacitySlots() {
        Requests.Slot missingStart = slotInput(1L, 2L, 6);
        missingStart.setStartTime(null);
        assertThrows(BusinessException.class, () -> service.create(missingStart));

        Requests.Slot missingEnd = slotInput(1L, 2L, 6);
        missingEnd.setEndTime(null);
        assertThrows(BusinessException.class, () -> service.create(missingEnd));

        Requests.Slot past = slotInput(1L, 2L, 6);
        past.setStartTime(LocalDateTime.now().minusDays(2));
        past.setEndTime(past.getStartTime().plusHours(1));
        assertThrows(BusinessException.class, () -> service.create(past));

        Requests.Slot crossDay = slotInput(1L, 2L, 6);
        crossDay.setEndTime(crossDay.getStartTime().plusDays(1));
        assertThrows(BusinessException.class, () -> service.create(crossDay));

        Requests.Slot reversed = slotInput(1L, 2L, 6);
        reversed.setEndTime(reversed.getStartTime().minusMinutes(1));
        assertThrows(BusinessException.class, () -> service.create(reversed));

        Requests.Slot overCapacityIndividual = slotInput(1L, 2L, 2);
        Course individual = course(1L);
        individual.setTypeCourse(TypeCourse.INDIVIDUAL);
        Instructor assigned = instructor(2L, individual);
        stubValidCreateFor(overCapacityIndividual, individual, assigned);
        assertThrows(BusinessException.class, () -> service.create(overCapacityIndividual));

        Requests.Slot zeroCapacity = slotInput(1L, 2L, 0);
        stubValidCreate(zeroCapacity);
        assertThrows(BusinessException.class, () -> service.create(zeroCapacity));

        Requests.Slot missingCapacity = slotInput(1L, 2L, 6);
        missingCapacity.setMaxCapacity(null);
        stubValidCreate(missingCapacity);
        assertThrows(BusinessException.class, () -> service.create(missingCapacity));

        Requests.Slot missingCourse = slotInput(1L, 2L, 1);
        when(courses.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.create(missingCourse));

        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void createAcceptsIndividualCapacityOfOne() {
        Requests.Slot input = slotInput(1L, 2L, 1);
        Course individualCourse = course(1L);
        individualCourse.setTypeCourse(TypeCourse.INDIVIDUAL);
        Instructor individualInstructor = instructor(2L, individualCourse);
        stubValidCreateFor(input, individualCourse, individualInstructor);
        when(slots.save(any(PlanningSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, service.create(input).getMaxCapacity());
    }

    @Test
    void createRejectsMissingInstructorAndAvailabilityOutsideWindow() {
        Requests.Slot input = slotInput(1L, 2L, 6);
        Course course = course(1L);
        when(courses.lockById(1L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.create(input));

        Instructor assigned = instructor(2L, course);
        when(instructors.lockById(2L)).thenReturn(Optional.of(assigned));
        when(instructors.existsById(2L)).thenReturn(true);
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.create(input));

        InstructorAvailability outsideWorkingHours = availability(assigned, input.getStartTime().toLocalDate());
        outsideWorkingHours.setStartTime(LocalTime.of(11, 0));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(outsideWorkingHours));
        assertThrows(BusinessException.class, () -> service.create(input));

        InstructorAvailability wrongDay = availability(assigned, input.getStartTime().toLocalDate());
        wrongDay.setDayOfWeek(DayOfWeek.of(input.getStartTime().getDayOfWeek().getValue() % 7 + 1));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(wrongDay));
        assertThrows(BusinessException.class, () -> service.create(input));

        InstructorAvailability tooEarlyDate = availability(assigned, input.getStartTime().toLocalDate());
        tooEarlyDate.setValidFrom(input.getStartTime().toLocalDate().plusDays(1));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(tooEarlyDate));
        assertThrows(BusinessException.class, () -> service.create(input));

        InstructorAvailability expiredDate = availability(assigned, input.getStartTime().toLocalDate());
        expiredDate.setValidUntil(input.getEndTime().toLocalDate().minusDays(1));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(expiredDate));
        assertThrows(BusinessException.class, () -> service.create(input));

        InstructorAvailability tooLateEnd = availability(assigned, input.getStartTime().toLocalDate());
        tooLateEnd.setEndTime(LocalTime.of(10, 30));
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(tooLateEnd));
        assertThrows(BusinessException.class, () -> service.create(input));
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void updatePersistsFutureSlotWithoutHistoryAndRejectsMissingSlot() {
        PlanningSlot current = slot(8L, LocalDateTime.now().plusDays(2));
        Requests.Slot update = slotInput(1L, 2L, 6);
        stubValidCreate(update);
        when(slots.lockById(8L)).thenReturn(Optional.of(current));
        when(registrations.findByPlanningSlot_Id(8L)).thenReturn(Collections.emptyList());
        when(slots.findAll()).thenReturn(Collections.singletonList(current));
        when(slots.save(current)).thenReturn(current);

        assertSame(current, service.update(8L, update));
        assertEquals(SlotStatus.OPEN, current.getStatus());
        assertThrows(BusinessException.class, () -> service.update(99L, update));
    }

    @Test
    void cancelRejectsMissingAndStartedSlots() {
        when(slots.lockById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.cancel(99L));
        when(slots.lockById(8L)).thenReturn(Optional.of(slot(8L, LocalDateTime.now().minusHours(1))));
        assertThrows(BusinessException.class, () -> service.cancel(8L));
    }

    @Test
    void searchValidatesPeriodAndOptionalResourceFilters() {
        LocalDateTime from = LocalDateTime.now().plusDays(1);
        LocalDateTime tomorrow = from;
        LocalDateTime dayAfterTomorrow = from.plusDays(1);
        assertThrows(BusinessException.class, () -> service.search(null, tomorrow, null, null, false));
        assertThrows(BusinessException.class, () -> service.search(tomorrow, null, null, null, false));
        assertThrows(BusinessException.class, () -> service.search(tomorrow, tomorrow, null, null, false));
        assertThrows(BusinessException.class, () -> service.search(tomorrow, dayAfterTomorrow, 0L, null, false));
        when(courses.existsById(1L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.search(tomorrow, dayAfterTomorrow, 1L, null, false));
        when(courses.existsById(1L)).thenReturn(true);
        when(instructors.existsById(2L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.search(tomorrow, dayAfterTomorrow, 1L, 2L, false));
    }

    @Test
    void searchAvailableKeepsOnlyFutureOpenSlotsAndSupportsNoResourceFilters() {
        LocalDateTime now = LocalDateTime.now();
        PlanningSlot futureOpen = slot(1L, now.plusDays(2));
        PlanningSlot futureCancelled = slot(2L, now.plusDays(3));
        futureCancelled.setStatus(SlotStatus.CANCELLED);
        PlanningSlot pastOpen = slot(3L, now.minusDays(2));
        when(slots.findAll()).thenReturn(Arrays.asList(futureCancelled, pastOpen, futureOpen));

        assertEquals(Collections.singletonList(futureOpen),
                service.search(now.minusDays(1), now.plusDays(5), null, null, true));
        assertEquals(3, service.search(now.minusDays(3), now.plusDays(5), null, null, false).size());
        assertEquals(1, service.search(now.minusDays(3), now.plusDays(5), null, null, true).size());
    }

    @Test
    void createSkipsSameSlotCancelledOtherInstructorAndNonOverlappingSlots() {
        Requests.Slot input = slotInput(1L, 2L, 6);
        stubValidCreate(input);
        Course requestedCourse = course(1L);
        Instructor requestedInstructor = instructor(2L, requestedCourse);
        PlanningSlot cancelled = slot(5L, input.getStartTime().minusMinutes(30));
        cancelled.setStatus(SlotStatus.CANCELLED);
        PlanningSlot otherInstructorSlot = slot(6L, input.getStartTime().minusMinutes(30));
        otherInstructorSlot.setInstructor(instructor(99L));
        PlanningSlot adjacent = slot(7L, input.getEndTime());
        for (PlanningSlot existing : Arrays.asList(cancelled, otherInstructorSlot, adjacent)) {
            existing.setInstructor(requestedInstructor);
        }
        otherInstructorSlot.getInstructor().setNumInstructor(99L);
        when(slots.findAll()).thenReturn(Arrays.asList(cancelled, otherInstructorSlot, adjacent));
        when(slots.save(any(PlanningSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(service.create(input));
    }

    private void stubValidCreate(Requests.Slot input) {
    Course course = course(1L);
    Instructor instructor = instructor(2L, course);
    when(courses.lockById(1L)).thenReturn(Optional.of(course));
    when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
    when(instructors.existsById(2L)).thenReturn(true);
    when(availabilities.findByInstructor_NumInstructor(2L))
            .thenReturn(Collections.singletonList(availability(instructor, input.getStartTime().toLocalDate())));
}

    private void stubValidCreateFor(Requests.Slot input, Course course, Instructor instructor) {
        when(courses.lockById(1L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
        when(instructors.existsById(2L)).thenReturn(true);
        when(availabilities.findByInstructor_NumInstructor(2L)).thenReturn(Collections.singletonList(
                availability(instructor, input.getStartTime().toLocalDate())));
    }

    private static Requests.Availability availabilityInput() {
        Requests.Availability input = new Requests.Availability();
        input.setDayOfWeek(DayOfWeek.MONDAY);
        input.setStartTime(LocalTime.of(9, 0));
        input.setEndTime(LocalTime.of(12, 0));
        input.setValidFrom(LocalDate.now().plusDays(1));
        input.setValidUntil(LocalDate.now().plusMonths(1));
        return input;
    }

    private static Requests.Slot slotInput(Long courseId, Long instructorId, int capacity) {
        Requests.Slot input = new Requests.Slot();
        input.setCourseId(courseId);
        input.setInstructorId(instructorId);
        LocalDate date = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        input.setStartTime(date.atTime(10, 0));
        input.setEndTime(date.atTime(11, 0));
        input.setMaxCapacity(capacity);
        return input;
    }

    private static InstructorAvailability availability(Instructor instructor, LocalDate date) {
        InstructorAvailability availability = new InstructorAvailability();
        availability.setInstructor(instructor);
        availability.setDayOfWeek(date.getDayOfWeek());
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(12, 0));
        availability.setValidFrom(date.minusDays(2));
        availability.setValidUntil(date.plusDays(2));
        return availability;
    }

    private static Course course(Long id) {
        Course course = new Course();
        course.setNumCourse(id);
        course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        course.setSupport(Support.SKI);
        course.setLevel(1);
        return course;
    }

    private static Instructor instructor(Long id, Course... courses) {
        Instructor instructor = new Instructor();
        instructor.setNumInstructor(id);
        instructor.setCourses(new HashSet<Course>(Arrays.asList(courses)));
        return instructor;
    }

    private static PlanningSlot slot(Long id, LocalDateTime start) {
        PlanningSlot slot = new PlanningSlot();
        slot.setId(id);
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setStatus(SlotStatus.OPEN);
        slot.setMaxCapacity(6);
        return slot;
    }
}

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
        Requests.Availability input = availabilityInput();
        input.setStartTime(LocalTime.of(12, 0));
        input.setEndTime(LocalTime.of(9, 0));

        assertThrows(BusinessException.class, () -> service.addAvailability(2L, input));
        verify(availabilities, never()).save(any(InstructorAvailability.class));
    }

    @Test
    void availabilityRejectsUnknownInstructor() {
        when(instructors.findById(99L)).thenReturn(Optional.empty());

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
        when(slots.lockById(8L)).thenReturn(Optional.of(current));
        when(registrations.findByPlanningSlot_Id(8L)).thenReturn(Collections.singletonList(new Registration()));

        assertThrows(BusinessException.class, () -> service.update(8L, slotInput(1L, 2L, 6)));

        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void updateRejectsCancelledOrStartedSlot() {
        PlanningSlot cancelled = slot(8L, LocalDateTime.now().plusDays(2));
        cancelled.setStatus(SlotStatus.CANCELLED);
        PlanningSlot started = slot(9L, LocalDateTime.now().minusHours(1));
        when(slots.lockById(8L)).thenReturn(Optional.of(cancelled));
        when(slots.lockById(9L)).thenReturn(Optional.of(started));

        assertThrows(BusinessException.class, () -> service.update(8L, slotInput(1L, 2L, 6)));
        assertThrows(BusinessException.class, () -> service.update(9L, slotInput(1L, 2L, 6)));
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
        when(slots.findAll()).thenReturn(Arrays.asList(later, earlier));
        when(courses.findById(1L)).thenReturn(Optional.of(selectedCourse));
        when(instructors.findById(2L)).thenReturn(Optional.of(selectedInstructor));

        List<PlanningSlot> result = service.search(from, from.plusDays(1), 1L, 2L, false);

        assertEquals(Arrays.asList(earlier, later), result);
    }

    private void stubValidCreate(Requests.Slot input) {
        Course course = course(1L);
        Instructor instructor = instructor(2L, course);
        when(courses.lockById(1L)).thenReturn(Optional.of(course));
        when(instructors.lockById(2L)).thenReturn(Optional.of(instructor));
        when(instructors.findById(2L)).thenReturn(Optional.of(instructor));
        when(availabilities.findByInstructor_NumInstructor(2L))
                .thenReturn(Collections.singletonList(availability(instructor, input.getStartTime().toLocalDate())));
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

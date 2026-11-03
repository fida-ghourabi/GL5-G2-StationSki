package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorServicesMockTest {

    @Mock
    private IInstructorRepository instructorRepository;
    @Mock
    private ICourseRepository courseRepository;
    @Mock
    private PlanningSlotRepository slotRepository;
    @Mock
    private InstructorAvailabilityRepository availabilityRepository;

    @InjectMocks
    private InstructorServicesImpl instructorServices;

    @Test
    void addInstructorSavesValidInstructor() {
        Instructor instructor = buildInstructor("Ali", "Ben Salah");
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor saved = instructorServices.addInstructor(instructor);

        assertNotNull(saved);
        assertEquals("Ali", saved.getFirstName());
        assertNotNull(saved.getCourses());
        assertTrue(saved.getCourses().isEmpty());
        verify(instructorRepository, times(1)).save(instructor);
    }

    @Test
    void retrieveAllInstructorsReturnsRepositoryData() {
        when(instructorRepository.findAll())
                .thenReturn(Arrays.asList(buildInstructor("Ali", "A"), buildInstructor("Sami", "B")));

        List<Instructor> result = instructorServices.retrieveAllInstructors();

        assertEquals(2, result.size());
        verify(instructorRepository, times(1)).findAll();
    }

    @Test
    void retrieveInstructorReturnsExistingInstructor() {
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        Instructor result = instructorServices.retrieveInstructor(1L);

        assertEquals(1L, result.getNumInstructor());
        verify(instructorRepository).findById(1L);
    }

    @Test
    void retrieveInstructorThrowsWhenMissing() {
        when(instructorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> instructorServices.retrieveInstructor(99L));
    }

    @Test
    void updateInstructorChangesNames() {
        Instructor current = buildInstructor("Ali", "A");
        current.setNumInstructor(1L);
        Instructor update = buildInstructor("Omar", "Z");
        update.setNumInstructor(1L);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(current));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = instructorServices.updateInstructor(update);

        assertEquals("Omar", result.getFirstName());
        assertEquals("Z", result.getLastName());
        verify(instructorRepository).save(current);
    }

    @Test
    void assignCourseLinksCourseToInstructor() {
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>());
        Course course = new Course();
        course.setNumCourse(2L);
        when(courseRepository.lockById(2L)).thenReturn(Optional.of(course));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(instructorRepository.findByCourses_NumCourse(2L))
                .thenReturn(Collections.<Instructor>emptyList());
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = instructorServices.assignCourse(1L, 2L);

        assertEquals(1, result.getCourses().size());
        verify(instructorRepository).save(instructor);
    }

    @Test
    void assignCourseRefusedWhenCourseAlreadyAssigned() {
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>());
        Course course = new Course();
        course.setNumCourse(2L);
        when(courseRepository.lockById(2L)).thenReturn(Optional.of(course));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(instructorRepository.findByCourses_NumCourse(2L))
                .thenReturn(Collections.singletonList(buildInstructor("Other", "B")));

        assertThrows(BusinessException.class, () -> instructorServices.assignCourse(1L, 2L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void unassignCourseRemovesLink() {
        Course course = new Course();
        course.setNumCourse(2L);
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>(Collections.singletonList(course)));
        when(courseRepository.lockById(2L)).thenReturn(Optional.of(course));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = instructorServices.unassignCourse(1L, 2L);

        assertTrue(result.getCourses().isEmpty());
        verify(instructorRepository).save(instructor);
    }

    @Test
    void removeInstructorDeletesWhenNoLinks() {
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>());
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        instructorServices.removeInstructor(1L);

        verify(instructorRepository, times(1)).delete(instructor);
    }

    @Test
    void removeInstructorRefusedWhenCoursesAssigned() {
        Instructor instructor = buildInstructor("Ali", "A");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>(Collections.singletonList(new Course())));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        assertThrows(BusinessException.class, () -> instructorServices.removeInstructor(1L));
        verify(instructorRepository, never()).delete(any(Instructor.class));
    }

    @Test
    void addInstructorRejectsInvalidIdProfileDateAndPreassignedCourses() {
        Instructor identified = buildInstructor("A", "B");
        identified.setNumInstructor(1L);
        assertThrows(BusinessException.class, () -> instructorServices.addInstructor(identified));

        Instructor futureHire = buildInstructor("A", "B");
        futureHire.setDateOfHire(LocalDate.now().plusDays(1));
        assertThrows(BusinessException.class, () -> instructorServices.addInstructor(futureHire));

        Instructor missingHireDate = buildInstructor("A", "B");
        missingHireDate.setDateOfHire(null);
        assertThrows(BusinessException.class, () -> instructorServices.addInstructor(missingHireDate));

        Instructor missingName = buildInstructor(" ", "B");
        assertThrows(BusinessException.class, () -> instructorServices.addInstructor(missingName));

        Instructor preassigned = buildInstructor("A", "B");
        preassigned.setCourses(new HashSet<>(Collections.singletonList(new Course())));
        assertThrows(BusinessException.class, () -> instructorServices.addInstructor(preassigned));
        verifyNoInteractions(instructorRepository);
    }

    @Test
    void addInstructorAcceptsAnEmptyCourseSet() {
        Instructor instructor = buildInstructor("Ali", "Ben Salah");
        instructor.setCourses(Collections.emptySet());
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        assertSame(instructor, instructorServices.addInstructor(instructor));
        assertTrue(instructor.getCourses().isEmpty());
        verify(instructorRepository).save(instructor);
    }

    @Test
    void updateInstructorAndRetrieveValidateMissingInstructor() {
        Instructor update = buildInstructor("New", "Name");
        update.setNumInstructor(4L);
        when(instructorRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> instructorServices.updateInstructor(update));

        assertThrows(BusinessException.class, () -> instructorServices.retrieveInstructor(0L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void assignCourseRejectsMissingCourseAndMissingInstructor() {
        when(courseRepository.lockById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> instructorServices.assignCourse(1L, 2L));

        when(courseRepository.lockById(2L)).thenReturn(Optional.of(new Course()));
        when(instructorRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> instructorServices.assignCourse(1L, 2L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void unassignCourseRejectsFutureActiveSlotButAllowsCancelledSlot() {
        Course course = new Course();
        course.setNumCourse(2L);
        Instructor instructor = buildInstructor("A", "B");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>(Collections.singletonList(course)));
        when(courseRepository.lockById(2L)).thenReturn(Optional.of(course));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        PlanningSlot futureSlot = matchingSlot(course, instructor, SlotStatus.OPEN,
                LocalDateTime.now().plusHours(1));
        when(slotRepository.findAll()).thenReturn(Collections.singletonList(futureSlot));
        assertThrows(BusinessException.class, () -> instructorServices.unassignCourse(1L, 2L));

        futureSlot.setStatus(SlotStatus.CANCELLED);
        Course otherCourse = new Course();
        otherCourse.setNumCourse(3L);
        Instructor otherInstructor = buildInstructor("Other", "Instructor");
        otherInstructor.setNumInstructor(8L);
        when(slotRepository.findAll()).thenReturn(Arrays.asList(
                matchingSlot(otherCourse, instructor, SlotStatus.OPEN, LocalDateTime.now().plusHours(2)),
                matchingSlot(course, otherInstructor, SlotStatus.OPEN, LocalDateTime.now().plusHours(2)),
                matchingSlot(course, instructor, SlotStatus.CANCELLED, LocalDateTime.now().plusHours(2)),
                matchingSlot(course, instructor, SlotStatus.OPEN, LocalDateTime.now().minusHours(2))));
        when(instructorRepository.save(instructor)).thenReturn(instructor);
        assertSame(instructor, instructorServices.unassignCourse(1L, 2L));
        assertTrue(instructor.getCourses().isEmpty());
    }

    @Test
    void unassignCourseRejectsMissingAssociation() {
        Course course = new Course();
        course.setNumCourse(2L);
        Instructor instructor = buildInstructor("A", "B");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>());
        when(courseRepository.lockById(2L)).thenReturn(Optional.of(course));
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(slotRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> instructorServices.unassignCourse(1L, 2L));
        verify(instructorRepository, never()).save(any(Instructor.class));
    }

    @Test
    void unassignCourseRejectsMissingCourse() {
        when(courseRepository.lockById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> instructorServices.unassignCourse(1L, 2L));
        verifyNoInteractions(instructorRepository, slotRepository);
    }

    @Test
    void removeInstructorRefusedWhenSlotsOrAvailabilityExist() {
        Instructor instructor = buildInstructor("A", "B");
        instructor.setNumInstructor(1L);
        instructor.setCourses(new HashSet<>());
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(slotRepository.existsByInstructor_NumInstructor(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> instructorServices.removeInstructor(1L));

        when(slotRepository.existsByInstructor_NumInstructor(1L)).thenReturn(false);
        when(availabilityRepository.existsByInstructor_NumInstructor(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> instructorServices.removeInstructor(1L));
        verify(instructorRepository, never()).delete(any(Instructor.class));
    }

    @Test
    void coursesAndSearchReturnExpectedResults() {
        Course course = new Course();
        Instructor first = buildInstructor("Ali", "Ben Salah");
        first.setNumInstructor(1L);
        first.setCourses(new HashSet<>(Collections.singletonList(course)));
        Instructor second = buildInstructor("Sami", "Other");
        second.setCourses(new HashSet<>());
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(first));
        when(instructorRepository.findAll()).thenReturn(Arrays.asList(first, second));

        assertEquals(first.getCourses(), instructorServices.courses(1L));
        assertEquals(Collections.singletonList(first), instructorServices.search("ben salah"));
        assertTrue(instructorServices.search("absent").isEmpty());
    }

    private PlanningSlot matchingSlot(Course course, Instructor instructor, SlotStatus status, LocalDateTime end) {
        PlanningSlot slot = new PlanningSlot();
        slot.setCourse(course);
        slot.setInstructor(instructor);
        slot.setStatus(status);
        slot.setEndTime(end);
        return slot;
    }

    private Instructor buildInstructor(String firstName, String lastName) {
        Instructor instructor = new Instructor();
        instructor.setFirstName(firstName);
        instructor.setLastName(lastName);
        instructor.setDateOfHire(LocalDate.of(2020, 1, 1));
        return instructor;
    }
}

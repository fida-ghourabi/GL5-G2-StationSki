package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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

    private Instructor buildInstructor(String firstName, String lastName) {
        Instructor instructor = new Instructor();
        instructor.setFirstName(firstName);
        instructor.setLastName(lastName);
        instructor.setDateOfHire(LocalDate.of(2020, 1, 1));
        return instructor;
    }
}
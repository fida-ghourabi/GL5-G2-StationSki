package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServicesMockTest {

    @Mock
    private ICourseRepository courseRepository;
    @Mock
    private IRegistrationRepository registrationRepository;
    @Mock
    private PlanningSlotRepository slotRepository;
    @Mock
    private IInstructorRepository instructorRepository;

    @InjectMocks
    private CourseServicesImpl courseServices;

    @Test
    void retrieveAllCoursesReturnsRepositoryData() {
        when(courseRepository.findAll())
                .thenReturn(Arrays.asList(buildCourse(), buildCourse()));

        List<Course> result = courseServices.retrieveAllCourses();

        assertEquals(2, result.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void addCourseSavesValidCourse() {
        Course course = buildCourse();
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course saved = courseServices.addCourse(course);

        assertNotNull(saved);
        assertEquals(course.getLevel(), saved.getLevel());
        assertNotNull(saved.getRegistrations());
        verify(courseRepository, times(1)).save(course);
    }

    @Test
    void retrieveCourseReturnsExistingCourse() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseServices.retrieveCourse(1L);

        assertEquals(1L, result.getNumCourse());
        verify(courseRepository).findById(1L);
    }

    @Test
    void retrieveCourseThrowsWhenMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> courseServices.retrieveCourse(99L));
    }

    @Test
    void updateCourseChangesPrice() {
        Course current = buildCourse();
        current.setNumCourse(1L);
        Course update = buildCourse();
        update.setNumCourse(1L);
        update.setPrice(200F);
        when(courseRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = courseServices.updateCourse(update);

        assertEquals(200F, result.getPrice());
        verify(courseRepository).save(current);
    }

    @Test
    void removeCourseDeletesWhenNoLinks() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(instructorRepository.findByCoursesNumCourse(1L))
                .thenReturn(Collections.<Instructor>emptyList());

        courseServices.removeCourse(1L);

        verify(courseRepository, times(1)).delete(course);
    }

    @Test
    void removeCourseRefusedWhenRegistrationsExist() {
        Course course = buildCourse();
        course.setNumCourse(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(registrationRepository.existsByCourseNumCourse(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> courseServices.removeCourse(1L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    private Course buildCourse() {
        Course course = new Course();
        course.setLevel(1);
        course.setTypeCourse(TypeCourse.INDIVIDUAL);
        course.setSupport(Support.SKI);
        course.setPrice(100F);
        course.setTimeSlot(1);
        return course;
    }
}
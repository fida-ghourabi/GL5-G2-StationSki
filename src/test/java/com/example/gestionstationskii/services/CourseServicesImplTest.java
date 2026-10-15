package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.repositories.ICourseRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de CourseServicesImpl.
 * Le repository est mocké : on teste uniquement la logique du service
 * (délégation, valeur retournée, branche orElse(null), propagation d'exception).
 */
@ExtendWith(MockitoExtension.class)
class CourseServicesImplTest {

    @Mock
    private ICourseRepository courseRepository;

    @InjectMocks
    private CourseServicesImpl courseServices;

    // ---------------------------------------------------------------
    // retrieveAllCourses
    // Partitions : liste non vide / liste vide
    // ---------------------------------------------------------------
    @Test
    @DisplayName("retrieveAllCourses : retourne tous les cours du repository")
    void retrieveAllCourses_shouldReturnAllCourses() {
        Course c1 = new Course();
        Course c2 = new Course();
        when(courseRepository.findAll()).thenReturn(Arrays.asList(c1, c2));

        List<Course> result = courseServices.retrieveAllCourses();

        assertEquals(2, result.size());
        assertTrue(result.contains(c1));
        assertTrue(result.contains(c2));
        verify(courseRepository, times(1)).findAll();
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    @DisplayName("retrieveAllCourses : retourne une liste vide si aucun cours")
    void retrieveAllCourses_shouldReturnEmptyList_whenNoCourse() {
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());

        List<Course> result = courseServices.retrieveAllCourses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(courseRepository).findAll();
    }

    // ---------------------------------------------------------------
    // addCourse
    // ---------------------------------------------------------------
    @Test
    @DisplayName("addCourse : sauvegarde le cours et retourne l'entité persistée")
    void addCourse_shouldSaveAndReturnCourse() {
        Course course = new Course();
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseServices.addCourse(course);

        assertSame(course, result);
        verify(courseRepository, times(1)).save(course);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    @DisplayName("addCourse : propage l'exception levée par le repository")
    void addCourse_shouldPropagateException_whenRepositoryFails() {
        Course course = new Course();
        when(courseRepository.save(course)).thenThrow(new IllegalArgumentException("invalid"));

        assertThrows(IllegalArgumentException.class, () -> courseServices.addCourse(course));
        verify(courseRepository).save(course);
    }

    // ---------------------------------------------------------------
    // updateCourse
    // ---------------------------------------------------------------
    @Test
    @DisplayName("updateCourse : sauvegarde le cours modifié et le retourne")
    void updateCourse_shouldSaveAndReturnUpdatedCourse() {
        Course course = new Course();
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseServices.updateCourse(course);

        assertSame(course, result);
        verify(courseRepository, times(1)).save(course);
        verifyNoMoreInteractions(courseRepository);
    }

    // ---------------------------------------------------------------
    // retrieveCourse
    // Couverture de branches : Optional présent / Optional vide
    // ---------------------------------------------------------------
    @Test
    @DisplayName("retrieveCourse : retourne le cours quand l'id existe")
    void retrieveCourse_shouldReturnCourse_whenFound() {
        Course course = new Course();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseServices.retrieveCourse(1L);

        assertSame(course, result);
        verify(courseRepository).findById(1L);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    @DisplayName("retrieveCourse : retourne null quand l'id n'existe pas")
    void retrieveCourse_shouldReturnNull_whenNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Course result = courseServices.retrieveCourse(99L);

        assertNull(result);
        verify(courseRepository).findById(99L);
    }

    @Test
    @DisplayName("retrieveCourse : propage l'exception du repository (id null)")
    void retrieveCourse_shouldPropagateException_whenIdIsNull() {
        when(courseRepository.findById(null)).thenThrow(new IllegalArgumentException("id must not be null"));

        assertThrows(IllegalArgumentException.class, () -> courseServices.retrieveCourse(null));
    }
}
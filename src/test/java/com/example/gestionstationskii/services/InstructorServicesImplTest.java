package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.repositories.ICourseRepository;
import com.example.gestionstationskii.repositories.IInstructorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de InstructorServicesImpl.
 */
@ExtendWith(MockitoExtension.class)
class InstructorServicesImplTest {

    @Mock
    private IInstructorRepository instructorRepository;

    @Mock
    private ICourseRepository courseRepository;

    @InjectMocks
    private InstructorServicesImpl instructorServices;

    // ---------------------------------------------------------------
    // addInstructor
    // ---------------------------------------------------------------
    @Test
    @DisplayName("addInstructor : sauvegarde et retourne l'instructeur")
    void addInstructor_shouldSaveAndReturnInstructor() {
        Instructor instructor = new Instructor();
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        Instructor result = instructorServices.addInstructor(instructor);

        assertSame(instructor, result);
        verify(instructorRepository, times(1)).save(instructor);
        verifyNoMoreInteractions(instructorRepository);
        verifyNoInteractions(courseRepository);
    }

    @Test
    @DisplayName("addInstructor : propage l'exception du repository")
    void addInstructor_shouldPropagateException() {
        Instructor instructor = new Instructor();
        when(instructorRepository.save(instructor)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> instructorServices.addInstructor(instructor));
    }

    // ---------------------------------------------------------------
    // retrieveAllInstructors
    // ---------------------------------------------------------------
    @Test
    @DisplayName("retrieveAllInstructors : retourne tous les instructeurs")
    void retrieveAllInstructors_shouldReturnList() {
        Instructor i1 = new Instructor();
        Instructor i2 = new Instructor();
        when(instructorRepository.findAll()).thenReturn(Arrays.asList(i1, i2));

        List<Instructor> result = instructorServices.retrieveAllInstructors();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(i1, i2)));
        verify(instructorRepository).findAll();
        verifyNoMoreInteractions(instructorRepository);
    }

    @Test
    @DisplayName("retrieveAllInstructors : retourne une liste vide si aucun instructeur")
    void retrieveAllInstructors_shouldReturnEmptyList() {
        when(instructorRepository.findAll()).thenReturn(Collections.emptyList());

        List<Instructor> result = instructorServices.retrieveAllInstructors();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // updateInstructor
    // ---------------------------------------------------------------
    @Test
    @DisplayName("updateInstructor : sauvegarde et retourne l'instructeur modifié")
    void updateInstructor_shouldSaveAndReturnInstructor() {
        Instructor instructor = new Instructor();
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        Instructor result = instructorServices.updateInstructor(instructor);

        assertSame(instructor, result);
        verify(instructorRepository, times(1)).save(instructor);
        verifyNoMoreInteractions(instructorRepository);
    }

    // ---------------------------------------------------------------
    // retrieveInstructor : branches Optional présent / vide
    // ---------------------------------------------------------------
    @Test
    @DisplayName("retrieveInstructor : retourne l'instructeur quand l'id existe")
    void retrieveInstructor_shouldReturnInstructor_whenFound() {
        Instructor instructor = new Instructor();
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        Instructor result = instructorServices.retrieveInstructor(1L);

        assertSame(instructor, result);
        verify(instructorRepository).findById(1L);
    }

    @Test
    @DisplayName("retrieveInstructor : retourne null quand l'id n'existe pas")
    void retrieveInstructor_shouldReturnNull_whenNotFound() {
        when(instructorRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(instructorServices.retrieveInstructor(99L));
        verify(instructorRepository).findById(99L);
    }

    // ---------------------------------------------------------------
    // addInstructorAndAssignToCourse
    // Partitions : cours existant / cours inexistant / instructeur null /
    //              instructeur ayant déjà des cours
    // ---------------------------------------------------------------
    @Test
    @DisplayName("addInstructorAndAssignToCourse : affecte le cours existant puis sauvegarde")
    void addInstructorAndAssignToCourse_shouldAssignCourse_whenCourseExists() {
        Instructor instructor = new Instructor();
        Course course = new Course();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        Instructor result = instructorServices.addInstructorAndAssignToCourse(instructor, 1L);

        assertNotNull(result);
        assertEquals(1, result.getCourses().size());
        assertTrue(result.getCourses().contains(course));

        ArgumentCaptor<Instructor> captor = ArgumentCaptor.forClass(Instructor.class);
        verify(instructorRepository, times(1)).save(captor.capture());
        assertSame(instructor, captor.getValue());

        verify(courseRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(courseRepository, instructorRepository);
    }

    @Test
    @DisplayName("addInstructorAndAssignToCourse : remplace les cours déjà associés")
    void addInstructorAndAssignToCourse_shouldReplaceExistingCourses() {
        Course oldCourse = new Course();
        Course newCourse = new Course();
        Instructor instructor = new Instructor();
        Set<Course> existing = new HashSet<>();
        existing.add(oldCourse);
        instructor.setCourses(existing);

        when(courseRepository.findById(2L)).thenReturn(Optional.of(newCourse));
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        Instructor result = instructorServices.addInstructorAndAssignToCourse(instructor, 2L);

        assertEquals(1, result.getCourses().size());
        assertTrue(result.getCourses().contains(newCourse));
        assertFalse(result.getCourses().contains(oldCourse));
    }

    /**
     * Test de caractérisation : documente le comportement ACTUEL.
     * Si le cours n'existe pas, le service ajoute null dans le Set et sauvegarde quand même.
     * Ce test révèle un défaut : voir recommandation de correction dans la réponse.
     */
    @Test
    @DisplayName("addInstructorAndAssignToCourse : cours inexistant -> le Set contient null (comportement actuel)")
    void addInstructorAndAssignToCourse_whenCourseNotFound_currentBehaviour() {
        Instructor instructor = new Instructor();
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        when(instructorRepository.save(instructor)).thenReturn(instructor);

        Instructor result = instructorServices.addInstructorAndAssignToCourse(instructor, 99L);

        assertEquals(1, result.getCourses().size());
        assertTrue(result.getCourses().contains(null));
        verify(instructorRepository).save(instructor);
    }

    @Test
    @DisplayName("addInstructorAndAssignToCourse : instructeur null -> NullPointerException, aucune sauvegarde")
    void addInstructorAndAssignToCourse_shouldThrowNPE_whenInstructorIsNull() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(new Course()));

        assertThrows(NullPointerException.class,
                () -> instructorServices.addInstructorAndAssignToCourse(null, 1L));

        verify(instructorRepository, never()).save(any());
    }

    @Test
    @DisplayName("addInstructorAndAssignToCourse : propage l'exception si findById échoue")
    void addInstructorAndAssignToCourse_shouldPropagateException_whenCourseRepositoryFails() {
        Instructor instructor = new Instructor();
        when(courseRepository.findById(null)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class,
                () -> instructorServices.addInstructorAndAssignToCourse(instructor, null));

        verify(instructorRepository, never()).save(any());
    }
}
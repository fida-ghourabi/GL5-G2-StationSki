package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.services.IInstructorServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorRestControllerTest {
    @Mock private IInstructorServices services;
    private InstructorRestController controller;

    @BeforeEach
    void setUp() { controller = new InstructorRestController(services); }

    @Test
    void delegatesInstructorEndpointsAndMapsRequestDto() {
        Long instructorId = 8L;
        Long courseId = 5L;
        Instructor instructor = new Instructor();
        List<Instructor> instructors = List.of(instructor);
        Set<Course> courses = Set.of(new Course());
        when(services.assignCourse(instructorId, courseId)).thenReturn(instructor);
        when(services.unassignCourse(instructorId, courseId)).thenReturn(instructor);
        when(services.courses(instructorId)).thenReturn(courses);
        when(services.search("Fida")).thenReturn(instructors);
        when(services.retrieveAllInstructors()).thenReturn(instructors);
        when(services.retrieveInstructor(instructorId)).thenReturn(instructor);
        when(services.addInstructor(any(Instructor.class))).thenAnswer(call -> call.getArgument(0));
        when(services.updateInstructor(any(Instructor.class))).thenAnswer(call -> call.getArgument(0));
        when(services.addInstructorAndAssignToCourse(any(Instructor.class), org.mockito.ArgumentMatchers.eq(courseId)))
                .thenAnswer(call -> call.getArgument(0));

        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setNumInstructor(instructorId);
        input.setFirstName("Fida");
        input.setLastName("Ghourabi");
        input.setDateOfHire(LocalDate.of(2024, 1, 1));

        assertSame(instructors, controller.getAllInstructors());
        assertSame(instructor, controller.getById(instructorId));
        assertEquals("Fida", controller.addInstructor(input).getFirstName());
        assertEquals("Fida", controller.updateInstructor(input).getFirstName());
        assertEquals("Fida", controller.addAndAssignToInstructor(input, courseId).getFirstName());
        assertSame(instructors, controller.search("Fida"));
        assertSame(instructor, controller.assign(instructorId, courseId));
        assertSame(instructor, controller.unassign(instructorId, courseId));
        assertSame(courses, controller.courses(instructorId));
        controller.delete(instructorId);

        verify(services).addInstructor(argThat(value -> value.getNumInstructor().equals(instructorId)
                && value.getFirstName().equals("Fida")));
        verify(services).addInstructorAndAssignToCourse(any(Instructor.class), org.mockito.ArgumentMatchers.eq(courseId));
        verify(services).removeInstructor(instructorId);
    }
}

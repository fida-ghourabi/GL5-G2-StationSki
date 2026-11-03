package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.entities.TypeCourse;
import com.example.gestionstationskii.services.ICourseServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRestControllerTest {
    @Mock private ICourseServices services;
    private CourseRestController controller;

    @BeforeEach
    void setUp() { controller = new CourseRestController(services); }

    @Test
    void delegatesCourseEndpointsAndMapsRequestDto() {
        Long id = 12L;
        Course course = new Course();
        List<Course> courses = List.of(course);
        List<Registration> registrations = List.of(new Registration());
        List<Instructor> instructors = List.of(new Instructor());
        when(services.retrieveAllCourses()).thenReturn(courses);
        when(services.retrieveCourse(id)).thenReturn(course);
        when(services.addCourse(any(Course.class))).thenAnswer(call -> call.getArgument(0));
        when(services.updateCourse(any(Course.class))).thenAnswer(call -> call.getArgument(0));
        when(services.search(TypeCourse.INDIVIDUAL, Support.SKI, 3)).thenReturn(courses);
        when(services.registrations(id)).thenReturn(registrations);
        when(services.participants(id)).thenReturn(4L);
        when(services.instructors(id)).thenReturn(instructors);

        Requests.CourseInput input = new Requests.CourseInput();
        input.setNumCourse(id);
        input.setLevel(3);
        input.setTypeCourse(TypeCourse.INDIVIDUAL);
        input.setSupport(Support.SKI);
        input.setPrice(120F);

        assertSame(courses, controller.getAllCourses());
        assertSame(course, controller.getById(id));
        assertEquals(id, controller.addCourse(input).getNumCourse());
        assertEquals(id, controller.updateCourse(input).getNumCourse());
        assertSame(courses, controller.search(TypeCourse.INDIVIDUAL, Support.SKI, 3));
        assertSame(registrations, controller.registrations(id));
        assertEquals(4L, controller.participants(id));
        assertSame(instructors, controller.instructors(id));
        controller.delete(id);

        verify(services).addCourse(argThat(value -> value.getNumCourse().equals(id)
                && value.getLevel() == 3 && value.getTypeCourse() == TypeCourse.INDIVIDUAL));
        verify(services).updateCourse(any(Course.class));
        verify(services).removeCourse(id);
    }
}

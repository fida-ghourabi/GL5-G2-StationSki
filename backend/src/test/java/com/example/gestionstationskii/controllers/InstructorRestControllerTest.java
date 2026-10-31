package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.IInstructorServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InstructorRestControllerTest {

    private static final String INSTRUCTOR_JSON =
            "{\"firstName\":\"Ali\",\"lastName\":\"Ben Salah\",\"dateOfHire\":\"2020-01-01\"}";

    @Mock
    private IInstructorServices instructorServices;

    @InjectMocks
    private InstructorRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllInstructorsReturnsList() throws Exception {
        when(instructorServices.retrieveAllInstructors())
                .thenReturn(Arrays.asList(buildInstructor(1L, "Ali"), buildInstructor(2L, "Sami")));

        mockMvc.perform(get("/instructor/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].firstName").value("Sami"));
    }

    @Test
    void getByIdReturnsInstructor() throws Exception {
        when(instructorServices.retrieveInstructor(1L)).thenReturn(buildInstructor(1L, "Ali"));

        mockMvc.perform(get("/instructor/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numInstructor").value(1))
                .andExpect(jsonPath("$.firstName").value("Ali"));
    }

    @Test
    void addInstructorDelegatesToService() throws Exception {
        when(instructorServices.addInstructor(any(Instructor.class)))
                .thenReturn(buildInstructor(5L, "Ali"));

        mockMvc.perform(post("/instructor/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSTRUCTOR_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numInstructor").value(5));

        verify(instructorServices).addInstructor(any(Instructor.class));
    }

    @Test
    void addAndAssignToCourseDelegatesToService() throws Exception {
        when(instructorServices.addInstructorAndAssignToCourse(any(Instructor.class), eq(3L)))
                .thenReturn(buildInstructor(6L, "Ali"));

        mockMvc.perform(put("/instructor/addAndAssignToCourse/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSTRUCTOR_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numInstructor").value(6));

        verify(instructorServices).addInstructorAndAssignToCourse(any(Instructor.class), eq(3L));
    }

    @Test
    void updateInstructorDelegatesToService() throws Exception {
        when(instructorServices.updateInstructor(any(Instructor.class)))
                .thenReturn(buildInstructor(1L, "Omar"));

        mockMvc.perform(put("/instructor/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSTRUCTOR_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Omar"));

        verify(instructorServices).updateInstructor(any(Instructor.class));
    }

    @Test
    void deleteInstructorDelegatesToService() throws Exception {
        mockMvc.perform(delete("/instructor/delete/7"))
                .andExpect(status().isOk());

        verify(instructorServices).removeInstructor(7L);
    }

    @Test
    void assignCourseDelegatesToService() throws Exception {
        when(instructorServices.assignCourse(1L, 2L)).thenReturn(buildInstructor(1L, "Ali"));

        mockMvc.perform(put("/instructor/1/courses/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numInstructor").value(1));

        verify(instructorServices).assignCourse(1L, 2L);
    }

    @Test
    void unassignCourseDelegatesToService() throws Exception {
        when(instructorServices.unassignCourse(1L, 2L)).thenReturn(buildInstructor(1L, "Ali"));

        mockMvc.perform(delete("/instructor/1/courses/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numInstructor").value(1));

        verify(instructorServices).unassignCourse(1L, 2L);
    }

    @Test
    void coursesReturnsInstructorCourses() throws Exception {
        Course course = new Course();
        course.setNumCourse(2L);
        course.setLevel(1);
        Set<Course> courses = new HashSet<Course>(Collections.singletonList(course));
        when(instructorServices.courses(1L)).thenReturn(courses);

        mockMvc.perform(get("/instructor/1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numCourse").value(2));
    }

    @Test
    void searchUsesNameParameter() throws Exception {
        when(instructorServices.search("ali"))
                .thenReturn(Collections.singletonList(buildInstructor(1L, "Ali")));

        mockMvc.perform(get("/instructor/search").param("name", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(instructorServices).search("ali");
    }

    @Test
    void searchWithoutNameUsesEmptyString() throws Exception {
        when(instructorServices.search("")).thenReturn(Collections.<Instructor>emptyList());

        mockMvc.perform(get("/instructor/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(instructorServices).search("");
    }

    private Instructor buildInstructor(Long id, String firstName) {
        Instructor instructor = new Instructor();
        instructor.setNumInstructor(id);
        instructor.setFirstName(firstName);
        instructor.setLastName("Test");
        return instructor;
    }
}
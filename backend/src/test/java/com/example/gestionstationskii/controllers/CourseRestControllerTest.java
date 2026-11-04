package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.ICourseServices;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseRestControllerTest {

    private static final String COURSE_JSON =
            "{\"level\":1,\"typeCourse\":\"INDIVIDUAL\",\"support\":\"SKI\",\"price\":100,\"timeSlot\":1}";

    @Mock
    private ICourseServices courseServices;

    @InjectMocks
    private CourseRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllCoursesReturnsList() throws Exception {
        when(courseServices.retrieveAllCourses())
                .thenReturn(Arrays.asList(buildCourse(1L), buildCourse(2L)));

        mockMvc.perform(get("/course/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numCourse").value(1));
    }

    @Test
    void getByIdReturnsCourse() throws Exception {
        when(courseServices.retrieveCourse(1L)).thenReturn(buildCourse(1L));

        mockMvc.perform(get("/course/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numCourse").value(1))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void addCourseDelegatesToService() throws Exception {
        when(courseServices.addCourse(any(Course.class))).thenReturn(buildCourse(5L));

        mockMvc.perform(post("/course/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numCourse").value(5));

        verify(courseServices).addCourse(any(Course.class));
    }

    @Test
    void updateCourseDelegatesToService() throws Exception {
        when(courseServices.updateCourse(any(Course.class))).thenReturn(buildCourse(1L));

        mockMvc.perform(put("/course/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COURSE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numCourse").value(1));

        verify(courseServices).updateCourse(any(Course.class));
    }

    @Test
    void deleteCourseDelegatesToService() throws Exception {
        mockMvc.perform(delete("/course/delete/7"))
                .andExpect(status().isOk());

        verify(courseServices).removeCourse(7L);
    }

    @Test
    void searchPassesFiltersToService() throws Exception {
        when(courseServices.search(TypeCourse.INDIVIDUAL, Support.SKI, 2))
                .thenReturn(Collections.singletonList(buildCourse(1L)));

        mockMvc.perform(get("/course/search")
                        .param("type", "INDIVIDUAL")
                        .param("support", "SKI")
                        .param("level", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(courseServices).search(TypeCourse.INDIVIDUAL, Support.SKI, 2);
    }

    @Test
    void searchWithoutFiltersPassesNulls() throws Exception {
        when(courseServices.search(null, null, null))
                .thenReturn(Collections.<Course>emptyList());

        mockMvc.perform(get("/course/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(courseServices).search(null, null, null);
    }

    @Test
    void registrationsReturnsHistory() throws Exception {
        when(courseServices.registrations(1L)).thenReturn(Collections.<Registration>emptyList());

        mockMvc.perform(get("/course/1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(courseServices).registrations(eq(1L));
    }

    @Test
    void participantsReturnsCount() throws Exception {
        when(courseServices.participants(1L)).thenReturn(3L);

        mockMvc.perform(get("/course/1/participants/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void instructorsReturnsAssignedInstructors() throws Exception {
        Instructor instructor = new Instructor();
        instructor.setNumInstructor(4L);
        instructor.setFirstName("Ali");
        when(courseServices.instructors(1L)).thenReturn(Collections.singletonList(instructor));

        mockMvc.perform(get("/course/1/instructors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Ali"));
    }

    private Course buildCourse(Long id) {
        Course course = new Course();
        course.setNumCourse(id);
        course.setLevel(1);
        course.setTypeCourse(TypeCourse.INDIVIDUAL);
        course.setSupport(Support.SKI);
        course.setPrice(100F);
        course.setTimeSlot(1);
        return course;
    }
}
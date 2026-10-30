package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("crud-test")
@Transactional
class InstructorServicesWithoutMockitoTest {

    @Autowired
    private IInstructorServices instructorServices;

    @Autowired
    private ICourseServices courseServices;

    @Autowired
    private IInstructorRepository instructorRepository;

    @Autowired
    private ICourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        instructorRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    void addInstructorPersistsInstructorWithGeneratedId() {
        Instructor saved = instructorServices.addInstructor(buildInstructor("Ali", "Ben Salah"));

        assertNotNull(saved.getNumInstructor());
        assertTrue(instructorRepository.findById(saved.getNumInstructor()).isPresent());
    }

    @Test
    void retrieveInstructorReturnsPersistedInstructor() {
        Instructor saved = instructorServices.addInstructor(buildInstructor("Sami", "Trabelsi"));

        Instructor found = instructorServices.retrieveInstructor(saved.getNumInstructor());

        assertEquals(saved.getNumInstructor(), found.getNumInstructor());
        assertEquals("Sami", found.getFirstName());
    }

    @Test
    void retrieveInstructorThrowsWhenMissing() {
        assertThrows(BusinessException.class, () -> instructorServices.retrieveInstructor(999999L));
    }

    @Test
    void retrieveAllInstructorsReturnsAllSavedInstructors() {
        instructorServices.addInstructor(buildInstructor("Ali", "A"));
        instructorServices.addInstructor(buildInstructor("Sami", "B"));

        assertEquals(2, instructorServices.retrieveAllInstructors().size());
    }

    @Test
    void updateInstructorChangesNames() {
        Instructor saved = instructorServices.addInstructor(buildInstructor("Ali", "A"));
        saved.setFirstName("Omar");
        saved.setLastName("Z");

        Instructor updated = instructorServices.updateInstructor(saved);

        assertEquals("Omar", updated.getFirstName());
        assertEquals("Z", instructorRepository.findById(saved.getNumInstructor()).get().getLastName());
    }

    @Test
    void searchFindsInstructorByName() {
        instructorServices.addInstructor(buildInstructor("Ali", "Ben Salah"));
        instructorServices.addInstructor(buildInstructor("Sami", "Trabelsi"));

        assertEquals(1, instructorServices.search("trabelsi").size());
        assertEquals(2, instructorServices.search("").size());
    }

    @Test
    void addInstructorAndAssignToCourseLinksCourse() {
        Course course = courseServices.addCourse(buildCourse());

        Instructor result = instructorServices.addInstructorAndAssignToCourse(
                buildInstructor("Ali", "A"), course.getNumCourse());

        assertEquals(1, result.getCourses().size());
        assertEquals(1, instructorServices.courses(result.getNumInstructor()).size());
    }

    @Test
    void removeInstructorDeletesPersistedInstructor() {
        Instructor saved = instructorServices.addInstructor(buildInstructor("Ali", "A"));
        Long id = saved.getNumInstructor();

        instructorServices.removeInstructor(id);

        assertFalse(instructorRepository.findById(id).isPresent());
    }

    private Instructor buildInstructor(String firstName, String lastName) {
        Instructor instructor = new Instructor();
        instructor.setFirstName(firstName);
        instructor.setLastName(lastName);
        instructor.setDateOfHire(LocalDate.of(2020, 1, 1));
        return instructor;
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
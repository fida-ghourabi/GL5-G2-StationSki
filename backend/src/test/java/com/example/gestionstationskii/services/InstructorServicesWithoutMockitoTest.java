package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class InstructorServicesWithoutMockitoTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("station_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
    }

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
    void retrieveAllInstructorsReturnsEmptyWhenNoInstructor() {
        assertTrue(instructorServices.retrieveAllInstructors().isEmpty());
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
    void updateInstructorThrowsWhenMissing() {
        Instructor unknown = buildInstructor("Omar", "Z");
        unknown.setNumInstructor(999999L);

        assertThrows(BusinessException.class, () -> instructorServices.updateInstructor(unknown));
    }

    @Test
    void searchFindsInstructorByName() {
        instructorServices.addInstructor(buildInstructor("Ali", "Ben Salah"));
        instructorServices.addInstructor(buildInstructor("Sami", "Trabelsi"));

        assertEquals(1, instructorServices.search("trabelsi").size());
        assertEquals(2, instructorServices.search("").size());
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        instructorServices.addInstructor(buildInstructor("Ali", "Ben Salah"));

        assertTrue(instructorServices.search("zzzzzz").isEmpty());
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
    void addInstructorAndAssignToCourseThrowsWhenCourseMissing() {
        assertThrows(BusinessException.class, () ->
                instructorServices.addInstructorAndAssignToCourse(buildInstructor("Ali", "A"), 999999L));
    }

    @Test
    void removeInstructorDeletesPersistedInstructor() {
        Instructor saved = instructorServices.addInstructor(buildInstructor("Ali", "A"));
        Long id = saved.getNumInstructor();

        instructorServices.removeInstructor(id);

        assertFalse(instructorRepository.findById(id).isPresent());
    }

    @Test
    void removeInstructorThrowsWhenMissing() {
        assertThrows(BusinessException.class, () -> instructorServices.removeInstructor(999999L));
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
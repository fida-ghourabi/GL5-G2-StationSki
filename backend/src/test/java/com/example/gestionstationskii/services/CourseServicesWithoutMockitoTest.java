package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServicesWithoutMockitoTest {

    @Autowired
    private ICourseServices courseServices;

    @Autowired
    private ICourseRepository courseRepository;

    @BeforeEach
    void cleanDatabase() {
        courseRepository.deleteAll();
    }

    @Test
    void addCoursePersistsCourseWithGeneratedId() {
        Course saved = courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 1));

        assertNotNull(saved.getNumCourse());
        assertTrue(courseRepository.findById(saved.getNumCourse()).isPresent());
    }

    @Test
    void retrieveCourseReturnsPersistedCourse() {
        Course saved = courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 2));

        Course found = courseServices.retrieveCourse(saved.getNumCourse());

        assertEquals(saved.getNumCourse(), found.getNumCourse());
        assertEquals(2, found.getLevel());
    }

    @Test
    void retrieveCourseThrowsWhenMissing() {
        assertThrows(BusinessException.class, () -> courseServices.retrieveCourse(999999L));
    }

    @Test
    void retrieveAllCoursesReturnsAllSavedCourses() {
        courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 1));
        courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SNOWBOARD, 2));

        assertEquals(2, courseServices.retrieveAllCourses().size());
    }

    @Test
    void updateCourseChangesPrice() {
        Course saved = courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 1));
        saved.setPrice(250F);

        Course updated = courseServices.updateCourse(saved);

        assertEquals(250F, updated.getPrice());
        assertEquals(250F, courseRepository.findById(saved.getNumCourse()).get().getPrice());
    }

    @Test
    void searchFiltersBySupportAndLevel() {
        courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 1));
        courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SNOWBOARD, 1));
        courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 3));

        assertEquals(2, courseServices.search(null, Support.SKI, null).size());
        assertEquals(1, courseServices.search(null, Support.SKI, 3).size());
    }

    @Test
    void removeCourseDeletesPersistedCourse() {
        Course saved = courseServices.addCourse(buildCourse(TypeCourse.INDIVIDUAL, Support.SKI, 1));
        Long id = saved.getNumCourse();

        courseServices.removeCourse(id);

        assertFalse(courseRepository.findById(id).isPresent());
    }

    private Course buildCourse(TypeCourse type, Support support, int level) {
        Course course = new Course();
        course.setLevel(level);
        course.setTypeCourse(type);
        course.setSupport(support);
        course.setPrice(100F);
        course.setTimeSlot(1);
        return course;
    }
}
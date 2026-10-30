package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.Piste;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.Subscription;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.entities.TypeCourse;
import com.example.gestionstationskii.entities.TypeSubscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestsMappingTest {

    @Test
    void courseInputMapsAllFields() {
        Requests.CourseInput input = new Requests.CourseInput();
        input.setNumCourse(12L);
        input.setLevel(3);
        input.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        input.setSupport(Support.SNOWBOARD);
        input.setPrice(125.5f);
        input.setTimeSlot(2);

        Course course = input.toEntity();

        assertEquals(12L, course.getNumCourse());
        assertEquals(3, course.getLevel());
        assertEquals(TypeCourse.COLLECTIVE_ADULT, course.getTypeCourse());
        assertEquals(Support.SNOWBOARD, course.getSupport());
        assertEquals(125.5f, course.getPrice(), 0.001f);
        assertEquals(2, course.getTimeSlot());
    }

    @Test
    void instructorInputMapsFieldsAndLeavesCoursesUnsetWhenNull() {
        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setNumInstructor(8L);
        input.setFirstName("Ada");
        input.setLastName("Lovelace");
        input.setDateOfHire(LocalDate.of(2020, 1, 15));

        Instructor instructor = input.toEntity();

        assertEquals(8L, instructor.getNumInstructor());
        assertEquals("Ada", instructor.getFirstName());
        assertEquals("Lovelace", instructor.getLastName());
        assertEquals(LocalDate.of(2020, 1, 15), instructor.getDateOfHire());
        assertNull(instructor.getCourses());
    }

    @Test
    void instructorInputMapsReferencedCoursesWhenProvided() {
        Requests.CourseReference first = new Requests.CourseReference();
        first.setNumCourse(10L);
        Requests.CourseReference second = new Requests.CourseReference();
        second.setNumCourse(11L);
        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setCourses(new HashSet<>(Arrays.asList(first, second)));

        Set<Long> courseIds = input.toEntity().getCourses().stream()
                .map(Course::getNumCourse)
                .collect(Collectors.toSet());

        assertEquals(new HashSet<>(Arrays.asList(10L, 11L)), courseIds);
    }

    @Test
    void instructorInputMapsAnExplicitlyEmptyCourseSet() {
        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setCourses(Collections.emptySet());

        assertEquals(Collections.emptySet(), input.toEntity().getCourses());
    }

    @Test
    void courseReferenceCreatesAnEntityContainingOnlyItsId() {
        Requests.CourseReference input = new Requests.CourseReference();
        input.setNumCourse(25L);

        Course course = input.toEntity();

        assertEquals(25L, course.getNumCourse());
        assertNull(course.getTypeCourse());
        assertNull(course.getSupport());
    }

    @Test
    void pisteInputMapsAllFields() {
        Requests.PisteInput input = new Requests.PisteInput();
        input.setNumPiste(4L);
        input.setNamePiste("North face");
        input.setColor(Color.BLACK);
        input.setLength(2300);
        input.setSlope(38);

        Piste piste = input.toEntity();

        assertEquals(4L, piste.getNumPiste());
        assertEquals("North face", piste.getNamePiste());
        assertEquals(Color.BLACK, piste.getColor());
        assertEquals(2300, piste.getLength());
        assertEquals(38, piste.getSlope());
    }

    @Test
    void skierInputMapsFieldsAndLeavesSubscriptionUnsetWhenNull() {
        Requests.SkierInput input = new Requests.SkierInput();
        input.setNumSkier(31L);
        input.setFirstName("Sam");
        input.setLastName("Rider");
        input.setDateOfBirth(LocalDate.of(2001, 4, 8));
        input.setCity("Grenoble");

        Skier skier = input.toEntity();

        assertEquals(31L, skier.getNumSkier());
        assertEquals("Sam", skier.getFirstName());
        assertEquals("Rider", skier.getLastName());
        assertEquals(LocalDate.of(2001, 4, 8), skier.getDateOfBirth());
        assertEquals("Grenoble", skier.getCity());
        assertNull(skier.getSubscription());
    }

    @Test
    void skierInputMapsNestedSubscriptionWhenProvided() {
        Requests.SubscriptionInput subscriptionInput = new Requests.SubscriptionInput();
        subscriptionInput.setNumSub(7L);
        subscriptionInput.setStartDate(LocalDate.of(2026, 1, 1));
        subscriptionInput.setEndDate(LocalDate.of(2026, 12, 31));
        subscriptionInput.setPrice(300f);
        subscriptionInput.setTypeSub(TypeSubscription.ANNUAL);
        Requests.SkierInput input = new Requests.SkierInput();
        input.setSubscription(subscriptionInput);

        Subscription subscription = input.toEntity().getSubscription();

        assertEquals(7L, subscription.getNumSub());
        assertEquals(LocalDate.of(2026, 1, 1), subscription.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), subscription.getEndDate());
        assertEquals(300f, subscription.getPrice(), 0.001f);
        assertEquals(TypeSubscription.ANNUAL, subscription.getTypeSub());
    }

    @Test
    void subscriptionInputMapsAllFields() {
        Requests.SubscriptionInput input = new Requests.SubscriptionInput();
        input.setNumSub(9L);
        input.setStartDate(LocalDate.of(2026, 2, 1));
        input.setEndDate(LocalDate.of(2026, 5, 1));
        input.setPrice(85f);
        input.setTypeSub(TypeSubscription.MONTHLY);

        Subscription subscription = input.toEntity();

        assertEquals(9L, subscription.getNumSub());
        assertEquals(LocalDate.of(2026, 2, 1), subscription.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 1), subscription.getEndDate());
        assertEquals(85f, subscription.getPrice(), 0.001f);
        assertEquals(TypeSubscription.MONTHLY, subscription.getTypeSub());
    }
}

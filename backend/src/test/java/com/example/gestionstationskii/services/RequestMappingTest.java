package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/** DTO commands must preserve writable fields without inventing associations. */
class RequestMappingTest {
    @Test
    void mapsCourseFields() {
        Requests.CourseInput input = new Requests.CourseInput();
        input.setNumCourse(1L); input.setLevel(2); input.setTypeCourse(TypeCourse.INDIVIDUAL);
        input.setSupport(Support.SKI); input.setPrice(25f); input.setTimeSlot(3);
        Course entity = input.toEntity();
        assertEquals(1L, entity.getNumCourse()); assertEquals(2, entity.getLevel());
        assertEquals(TypeCourse.INDIVIDUAL, entity.getTypeCourse()); assertEquals(Support.SKI, entity.getSupport());
        assertEquals(25f, entity.getPrice()); assertEquals(3, entity.getTimeSlot());
    }

    @Test
    void mapsInstructorWithCourseReferencesAndWithoutCourses() {
        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setNumInstructor(2L); input.setFirstName("Test"); input.setLastName("Instructor");
        input.setDateOfHire(LocalDate.of(2020, 1, 1));
        Instructor without = input.toEntity();
        assertEquals(2L, without.getNumInstructor()); assertEquals("Test", without.getFirstName());
        assertEquals("Instructor", without.getLastName()); assertEquals(input.getDateOfHire(), without.getDateOfHire());
        assertNull(without.getCourses());
        Requests.CourseReference reference = new Requests.CourseReference(); reference.setNumCourse(3L);
        input.setCourses(Collections.singleton(reference));
        Instructor with = input.toEntity();
        assertEquals(1, with.getCourses().size());
        Course linked = with.getCourses().iterator().next();
        assertEquals(3L, linked.getNumCourse()); assertNull(linked.getPrice());
        input.setCourses(Collections.emptySet());
        assertTrue(input.toEntity().getCourses().isEmpty());
    }

    @Test
    void mapsPisteFields() {
        Requests.PisteInput input = new Requests.PisteInput();
        input.setNumPiste(4L); input.setNamePiste("Blue run"); input.setColor(Color.BLUE);
        input.setLength(1000); input.setSlope(25);
        Piste entity = input.toEntity();
        assertEquals(4L, entity.getNumPiste()); assertEquals("Blue run", entity.getNamePiste());
        assertEquals(Color.BLUE, entity.getColor()); assertEquals(1000, entity.getLength());
        assertEquals(25, entity.getSlope()); assertNull(entity.getSkiers());
    }

    @Test
    void mapsSkierAndOptionalSubscription() {
        Requests.SkierInput input = new Requests.SkierInput();
        input.setNumSkier(5L); input.setFirstName("Test"); input.setLastName("Skier");
        input.setDateOfBirth(LocalDate.of(2000, 1, 1)); input.setCity("Test city");
        Skier entity = input.toEntity();
        assertEquals(5L, entity.getNumSkier()); assertEquals("Test", entity.getFirstName());
        assertEquals("Skier", entity.getLastName()); assertEquals(input.getDateOfBirth(), entity.getDateOfBirth());
        assertEquals("Test city", entity.getCity()); assertNull(entity.getSubscription());
        Requests.SubscriptionInput sub = new Requests.SubscriptionInput();
        sub.setNumSub(6L); sub.setStartDate(LocalDate.of(2030, 1, 1));
        sub.setEndDate(LocalDate.of(2030, 2, 1)); sub.setPrice(50f); sub.setTypeSub(TypeSubscription.MONTHLY);
        input.setSubscription(sub);
        Subscription mapped = input.toEntity().getSubscription();
        assertEquals(6L, mapped.getNumSub()); assertEquals(sub.getStartDate(), mapped.getStartDate());
        assertEquals(sub.getEndDate(), mapped.getEndDate()); assertEquals(50f, mapped.getPrice());
        assertEquals(TypeSubscription.MONTHLY, mapped.getTypeSub());
    }
}

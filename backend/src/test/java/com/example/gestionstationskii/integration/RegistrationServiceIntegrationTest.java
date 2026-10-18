package com.example.gestionstationskii.integration;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.PlanningSlot;
import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.entities.SlotStatus;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.entities.TypeCourse;
import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.IRegistrationServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationServiceIntegrationTest extends RegistrationIntegrationSupport {
    @Autowired private IRegistrationServices registrationServices;

    @Test
    void persistedRegistrationsEnforceWeeklyRulesAndCanBeSearched() {
        Course course = createCourse();
        Instructor instructor = createInstructor(course);
        Skier skier = createAdultSkier();
        LocalDateTime monday = LocalDate.now().plusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(10, 0);
        PlanningSlot first = createSlotAt(course, instructor, monday, 4);
        PlanningSlot sameWeek = createSlotAt(course, instructor, monday.plusHours(4), 4);
        PlanningSlot nextWeek = createSlotAt(course, instructor, monday.plusWeeks(1), 4);

        Registration firstRegistration = registrationServices.registerSlot(skier.getNumSkier(), first.getId());
        assertThrows(BusinessException.class,
                () -> registrationServices.registerSlot(skier.getNumSkier(), sameWeek.getId()));
        Registration nextWeekRegistration = registrationServices.registerSlot(skier.getNumSkier(), nextWeek.getId());

        List<Registration> results = registrationServices.search(monday.minusHours(1), monday.plusHours(2), course.getNumCourse());
        assertEquals(1, results.size());
        assertEquals(firstRegistration.getNumRegistration(), results.get(0).getNumRegistration());
        assertNotNull(registrations.findById(nextWeekRegistration.getNumRegistration()).orElse(null));
        assertEquals(SlotStatus.OPEN, slots.findById(sameWeek.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    void persistedAgeRulesRejectChildrenFromAdultCoursesAndAllowThemOnChildrenCourses() {
        Course adultCourse = createCourse(TypeCourse.COLLECTIVE_ADULT);
        Course childrenCourse = createCourse(TypeCourse.COLLECTIVE_CHILDREN);
        Instructor adultInstructor = createInstructor(adultCourse);
        Instructor childrenInstructor = createInstructor(childrenCourse);
        LocalDateTime start = LocalDate.now().plusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(10, 0);
        PlanningSlot adultSlot = createSlotAt(adultCourse, adultInstructor, start, 2);
        PlanningSlot childrenSlot = createSlotAt(childrenCourse, childrenInstructor, start.plusHours(2), 2);
        Skier child = new Skier();
        child.setDateOfBirth(start.toLocalDate().minusYears(15));
        child = skiers.saveAndFlush(child);
        Long childId = child.getNumSkier();

        assertThrows(BusinessException.class, () -> registrationServices.registerSlot(childId, adultSlot.getId()));
        assertNotNull(registrationServices.registerSlot(childId, childrenSlot.getId()));
    }
}

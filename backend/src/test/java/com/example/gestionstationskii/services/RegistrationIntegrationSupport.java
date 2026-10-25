package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;

@SpringBootTest
abstract class RegistrationIntegrationSupport {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("station_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("server.servlet.context-path", () -> "");
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
    }

    @Autowired protected IRegistrationRepository registrations;
    @Autowired protected ISkierRepository skiers;
    @Autowired protected ICourseRepository courses;
    @Autowired protected IInstructorRepository instructors;
    @Autowired protected PlanningSlotRepository slots;

    protected Course createCourse() {
        return createCourse(TypeCourse.COLLECTIVE_ADULT);
    }

    protected Course createCourse(TypeCourse type) {
        Course course = new Course();
        course.setTypeCourse(type);
        course.setSupport(Support.SKI);
        course.setLevel(1);
        course.setPrice(50f);
        return courses.save(course);
    }

    protected Instructor createInstructor(Course course) {
        Instructor instructor = new Instructor();
        instructor.setCourses(Collections.singleton(course));
        return instructors.save(instructor);
    }

    protected Skier createAdultSkier() {
        Skier skier = new Skier();
        skier.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return skiers.save(skier);
    }

    protected PlanningSlot createSlot(Course course, Instructor instructor, int capacity) {
        LocalDate nextMonday = LocalDate.now().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        return createSlotAt(course, instructor, nextMonday.atTime(10, 0), capacity);
    }

    protected PlanningSlot createSlotAt(Course course, Instructor instructor, LocalDateTime start, int capacity) {
        PlanningSlot slot = new PlanningSlot();
        slot.setCourse(course);
        slot.setInstructor(instructor);
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setMaxCapacity(capacity);
        slot.setStatus(SlotStatus.OPEN);
        return slots.saveAndFlush(slot);
    }
}

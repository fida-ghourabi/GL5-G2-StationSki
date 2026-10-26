package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.HashSet;
import java.util.Set;

/** Commands expose identifiers, never writable JPA associations or computed fields. */
public final class Requests {
    private Requests() { }
    @Getter @Setter
    public static class RegistrationInput {
        private Long numRegistration;
        private int numWeek;
        private PlanningSlotReference planningSlot;
    }
    @Getter @Setter
    public static class PlanningSlotReference {
        private Long id;
    }
    @Getter @Setter
    public static class Availability {
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private LocalDate validFrom;
        private LocalDate validUntil;
    }
    @Getter @Setter
    public static class Slot {
        private Long courseId;
        private Long instructorId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer maxCapacity;
    }
    @Getter @Setter
    public static class PaymentInput {
        private Long skierId;
        private Long subscriptionId;
        private BigDecimal amount;
        private PaymentMethod method;
        private String transactionReference;
    }
    @Getter @Setter
    public static class PaymentState {
        private PaymentStatus status;
    }
    @Getter @Setter
    public static class RefundInput {
        private BigDecimal amount;
        private String reason;
    }

    @Getter @Setter
    public static class CourseInput {
        private Long numCourse;
        private int level;
        private TypeCourse typeCourse;
        private Support support;
        private Float price;
        private int timeSlot;

        public Course toEntity() {
            Course course = new Course();
            course.setNumCourse(numCourse);
            course.setLevel(level);
            course.setTypeCourse(typeCourse);
            course.setSupport(support);
            course.setPrice(price);
            course.setTimeSlot(timeSlot);
            return course;
        }
    }

    @Getter @Setter
    public static class InstructorInput {
        private Long numInstructor;
        private String firstName;
        private String lastName;
        private LocalDate dateOfHire;
        private Set<CourseReference> courses;

        public Instructor toEntity() {
            Instructor instructor = new Instructor();
            instructor.setNumInstructor(numInstructor);
            instructor.setFirstName(firstName);
            instructor.setLastName(lastName);
            instructor.setDateOfHire(dateOfHire);
            if (courses != null) {
                Set<Course> courseEntities = new HashSet<>();
                for (CourseReference course : courses) {
                    courseEntities.add(course.toEntity());
                }
                instructor.setCourses(courseEntities);
            }
            return instructor;
        }
    }

    @Getter @Setter
    public static class CourseReference {
        private Long numCourse;

        public Course toEntity() {
            Course course = new Course();
            course.setNumCourse(numCourse);
            return course;
        }
    }

    @Getter @Setter
    public static class PisteInput {
        private Long numPiste;
        private String namePiste;
        private Color color;
        private int length;
        private int slope;

        public Piste toEntity() {
            Piste piste = new Piste();
            piste.setNumPiste(numPiste);
            piste.setNamePiste(namePiste);
            piste.setColor(color);
            piste.setLength(length);
            piste.setSlope(slope);
            return piste;
        }
    }

    @Getter @Setter
    public static class SkierInput {
        private Long numSkier;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String city;
        private SubscriptionInput subscription;

        public Skier toEntity() {
            Skier skier = new Skier();
            skier.setNumSkier(numSkier);
            skier.setFirstName(firstName);
            skier.setLastName(lastName);
            skier.setDateOfBirth(dateOfBirth);
            skier.setCity(city);
            if (subscription != null) {
                skier.setSubscription(subscription.toEntity());
            }
            return skier;
        }
    }

    @Getter @Setter
    public static class SubscriptionInput {
        private Long numSub;
        private LocalDate startDate;
        private LocalDate endDate;
        private Float price;
        private TypeSubscription typeSub;

        public Subscription toEntity() {
            Subscription subscriptionEntity = new Subscription();
            subscriptionEntity.setNumSub(numSub);
            subscriptionEntity.setStartDate(startDate);
            subscriptionEntity.setEndDate(endDate);
            subscriptionEntity.setPrice(price);
            subscriptionEntity.setTypeSub(typeSub);
            return subscriptionEntity;
        }
    }
}

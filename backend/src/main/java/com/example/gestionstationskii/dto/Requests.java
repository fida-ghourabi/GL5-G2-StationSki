package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;

/** Commands expose identifiers, never writable JPA associations or computed fields. */
public final class Requests {
    private Requests() { }
    @Getter @Setter
    public static class CourseInput {
        private Long numCourse;
        private int level;
        private TypeCourse typeCourse;
        private Support support;
        private Float price;
    }
    @Getter @Setter
    public static class InstructorInput {
        private Long numInstructor;
        private String firstName;
        private String lastName;
        private LocalDate dateOfHire;
    }
    @Getter @Setter
    public static class PisteInput {
        private Long numPiste;
        private String namePiste;
        private Color color;
        private int length;
        private int slope;
    }
    @Getter @Setter
    public static class RegistrationInput {
        private int numWeek;
        private PlanningSlotReference planningSlot;
    }
    @Getter @Setter
    public static class PlanningSlotReference {
        private Long id;
    }
    @Getter @Setter
    public static class SkierInput {
        private Long numSkier;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String city;
        private SubscriptionInput subscription;
    }
    @Getter @Setter
    public static class SubscriptionInput {
        private Long numSub;
        private LocalDate startDate;
        private Float price;
        private TypeSubscription typeSub;
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
}

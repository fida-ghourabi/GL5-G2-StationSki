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
}

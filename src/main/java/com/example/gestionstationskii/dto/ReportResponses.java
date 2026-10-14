package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ReportResponses {
    private ReportResponses() { }
    @Getter @AllArgsConstructor
    public static class Revenue {
        private final LocalDateTime from;
        private final LocalDateTime to;
        private final BigDecimal collected;
        private final BigDecimal refunds;
        private final BigDecimal net;
        private final List<MonthlyRevenue> months;
    }
    @Getter @AllArgsConstructor
    public static class MonthlyRevenue {
        private final String month;
        private final BigDecimal collected;
        private final BigDecimal refunds;
        private final BigDecimal net;
    }
    @Getter @AllArgsConstructor
    public static class Subscriptions {
        private final LocalDate asOf;
        private final long active;
        private final long expired;
        private final long upcoming;
        private final long expiringWithin30Days;
    }
    @Getter @AllArgsConstructor
    public static class CourseOccupancy {
        private final Long courseId;
        private final long slotCount;
        private final long capacity;
        private final long registrations;
        private final BigDecimal occupancyPercent;
    }
    @Getter @AllArgsConstructor
    public static class WeeklyRegistrations {
        private final Long courseId;
        private final Integer weekBasedYear;
        private final int week;
        private final long registrations;
    }
    @Getter @AllArgsConstructor
    public static class Receipt {
        private final String receiptNumber;
        private final Long paymentId;
        private final Long skierId;
        private final String skierName;
        private final Long subscriptionId;
        private final LocalDateTime createdAt;
        private final LocalDateTime paidAt;
        private final PaymentMethod method;
        private final PaymentStatus status;
        private final BigDecimal amount;
        private final BigDecimal refundedAmount;
        private final BigDecimal netAmount;
        private final List<Refund> refunds;
    }
}

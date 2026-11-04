package com.example.gestionstationskii.dto;

import com.example.gestionstationskii.entities.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RequestsTest {

    @Test
    void courseInputConvertsToEntity() {
        Requests.CourseInput in = new Requests.CourseInput();
        in.setNumCourse(4L);
        in.setLevel(2);
        in.setTypeCourse(TypeCourse.INDIVIDUAL);
        in.setSupport(Support.SNOWBOARD);
        in.setPrice(49.5f);
        in.setTimeSlot(3);

        Course c = in.toEntity();

        assertEquals(Long.valueOf(4L), c.getNumCourse());
        assertEquals(2, c.getLevel());
        assertEquals(TypeCourse.INDIVIDUAL, c.getTypeCourse());
        assertEquals(Support.SNOWBOARD, c.getSupport());
        assertEquals(Float.valueOf(49.5f), c.getPrice());
        assertEquals(3, c.getTimeSlot());
        assertEquals(4L, in.getNumCourse().longValue());
    }

    @Test
    void instructorInputConvertsToEntity() {
        Requests.InstructorInput in = new Requests.InstructorInput();
        in.setNumInstructor(5L);
        in.setFirstName("Ali");
        in.setLastName("Ben Salah");
        in.setDateOfHire(LocalDate.of(2020, 9, 1));

        Instructor i = in.toEntity();

        assertEquals(Long.valueOf(5L), i.getNumInstructor());
        assertEquals("Ali", i.getFirstName());
        assertEquals("Ben Salah", i.getLastName());
        assertEquals(LocalDate.of(2020, 9, 1), i.getDateOfHire());
    }

    @Test
    void pisteInputConvertsToEntity() {
        Requests.PisteInput in = new Requests.PisteInput();
        in.setNumPiste(6L);
        in.setNamePiste("Aigle");
        in.setColor(Color.BLACK);
        in.setLength(2400);
        in.setSlope(38);

        Piste p = in.toEntity();

        assertEquals(Long.valueOf(6L), p.getNumPiste());
        assertEquals("Aigle", p.getNamePiste());
        assertEquals(Color.BLACK, p.getColor());
        assertEquals(2400, p.getLength());
        assertEquals(38, p.getSlope());
    }

    @Test
    void subscriptionInputConvertsToEntity() {
        Requests.SubscriptionInput in = new Requests.SubscriptionInput();
        in.setNumSub(7L);
        in.setStartDate(LocalDate.of(2030, 1, 1));
        in.setEndDate(LocalDate.of(2030, 12, 31));
        in.setPrice(300f);
        in.setTypeSub(TypeSubscription.ANNUAL);

        Subscription s = in.toEntity();

        assertEquals(Long.valueOf(7L), s.getNumSub());
        assertEquals(LocalDate.of(2030, 1, 1), s.getStartDate());
        assertEquals(LocalDate.of(2030, 12, 31), s.getEndDate());
        assertEquals(Float.valueOf(300f), s.getPrice());
        assertEquals(TypeSubscription.ANNUAL, s.getTypeSub());
    }

    @Test
    void skierInputWithoutSubscriptionHasNoSubscription() {
        Requests.SkierInput in = new Requests.SkierInput();
        in.setNumSkier(8L);
        in.setFirstName("Sahar");
        in.setLastName("Saidani");
        in.setCity("Tunis");
        in.setDateOfBirth(LocalDate.of(2001, 2, 3));

        Skier s = in.toEntity();

        assertEquals(Long.valueOf(8L), s.getNumSkier());
        assertEquals("Sahar", s.getFirstName());
        assertEquals("Saidani", s.getLastName());
        assertEquals("Tunis", s.getCity());
        assertEquals(LocalDate.of(2001, 2, 3), s.getDateOfBirth());
        assertNull(s.getSubscription());
    }

    @Test
    void skierInputWithSubscriptionConvertsNestedSubscription() {
        Requests.SubscriptionInput sub = new Requests.SubscriptionInput();
        sub.setTypeSub(TypeSubscription.MONTHLY);
        sub.setPrice(40f);
        Requests.SkierInput in = new Requests.SkierInput();
        in.setSubscription(sub);

        Skier s = in.toEntity();

        assertNotNull(s.getSubscription());
        assertEquals(TypeSubscription.MONTHLY, s.getSubscription().getTypeSub());
        assertEquals(Float.valueOf(40f), s.getSubscription().getPrice());
        assertSame(sub, in.getSubscription());
    }

    @Test
    void registrationInputWithoutSlotHasNoSlot() {
        Requests.RegistrationInput in = new Requests.RegistrationInput();
        in.setNumRegistration(9L);
        in.setNumWeek(12);

        Registration r = in.toEntity();

        assertEquals(Long.valueOf(9L), r.getNumRegistration());
        assertEquals(12, r.getNumWeek());
        assertNull(r.getPlanningSlot());
    }

    @Test
    void registrationInputWithSlotReferenceBuildsSlotWithIdOnly() {
        Requests.SlotRef ref = new Requests.SlotRef();
        ref.setId(21L);
        Requests.RegistrationInput in = new Requests.RegistrationInput();
        in.setPlanningSlot(ref);

        Registration r = in.toEntity();

        assertNotNull(r.getPlanningSlot());
        assertEquals(Long.valueOf(21L), r.getPlanningSlot().getId());
        assertNull(r.getPlanningSlot().getCourse());
        assertEquals(Long.valueOf(21L), in.getPlanningSlot().getId());
    }

    @Test
    void availabilityAndSlotCommandsExposeTheirValues() {
        Requests.Availability a = new Requests.Availability();
        a.setDayOfWeek(DayOfWeek.FRIDAY);
        a.setStartTime(LocalTime.of(9, 0));
        a.setEndTime(LocalTime.of(17, 0));
        a.setValidFrom(LocalDate.of(2030, 1, 1));
        a.setValidUntil(LocalDate.of(2030, 3, 1));
        assertEquals(DayOfWeek.FRIDAY, a.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), a.getStartTime());
        assertEquals(LocalTime.of(17, 0), a.getEndTime());
        assertEquals(LocalDate.of(2030, 1, 1), a.getValidFrom());
        assertEquals(LocalDate.of(2030, 3, 1), a.getValidUntil());

        Requests.Slot s = new Requests.Slot();
        s.setCourseId(1L);
        s.setInstructorId(2L);
        s.setStartTime(LocalDateTime.of(2030, 1, 4, 10, 0));
        s.setEndTime(LocalDateTime.of(2030, 1, 4, 11, 0));
        s.setMaxCapacity(5);
        assertEquals(Long.valueOf(1L), s.getCourseId());
        assertEquals(Long.valueOf(2L), s.getInstructorId());
        assertEquals(LocalDateTime.of(2030, 1, 4, 10, 0), s.getStartTime());
        assertEquals(LocalDateTime.of(2030, 1, 4, 11, 0), s.getEndTime());
        assertEquals(Integer.valueOf(5), s.getMaxCapacity());
    }

    @Test
    void paymentCommandsExposeTheirValues() {
        Requests.PaymentInput p = new Requests.PaymentInput();
        p.setSkierId(1L);
        p.setSubscriptionId(2L);
        p.setAmount(new BigDecimal("150.00"));
        p.setMethod(PaymentMethod.CARD);
        p.setTransactionReference("TX-1");
        assertEquals(Long.valueOf(1L), p.getSkierId());
        assertEquals(Long.valueOf(2L), p.getSubscriptionId());
        assertEquals(new BigDecimal("150.00"), p.getAmount());
        assertEquals(PaymentMethod.CARD, p.getMethod());
        assertEquals("TX-1", p.getTransactionReference());

        Requests.PaymentState state = new Requests.PaymentState();
        state.setStatus(PaymentStatus.PAID);
        assertEquals(PaymentStatus.PAID, state.getStatus());

        Requests.RefundInput refund = new Requests.RefundInput();
        refund.setAmount(new BigDecimal("10.00"));
        refund.setReason("Erreur");
        assertEquals(new BigDecimal("10.00"), refund.getAmount());
        assertEquals("Erreur", refund.getReason());
    }

    @Test
    void paymentReceiptExposesItsValues() {
        LocalDateTime created = LocalDateTime.of(2030, 1, 1, 10, 0);
        LocalDateTime paid = LocalDateTime.of(2030, 1, 1, 10, 5);
        Refund refund = new Refund();
        PaymentReceipt receipt = new PaymentReceipt("R-1", 1L, 2L, "Sahar Saidani", 3L, created, paid,
                PaymentMethod.CASH, PaymentStatus.PARTIALLY_REFUNDED, new BigDecimal("100.00"),
                new BigDecimal("30.00"), new BigDecimal("70.00"), Collections.singletonList(refund));

        assertEquals("R-1", receipt.getReceiptNumber());
        assertEquals(Long.valueOf(1L), receipt.getPaymentId());
        assertEquals(Long.valueOf(2L), receipt.getSkierId());
        assertEquals("Sahar Saidani", receipt.getSkierName());
        assertEquals(Long.valueOf(3L), receipt.getSubscriptionId());
        assertEquals(created, receipt.getCreatedAt());
        assertEquals(paid, receipt.getPaidAt());
        assertEquals(PaymentMethod.CASH, receipt.getMethod());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, receipt.getStatus());
        assertEquals(new BigDecimal("100.00"), receipt.getAmount());
        assertEquals(new BigDecimal("30.00"), receipt.getRefundedAmount());
        assertEquals(new BigDecimal("70.00"), receipt.getNetAmount());
        assertEquals(Arrays.asList(refund), receipt.getRefunds());
    }

    @Test
    void containerClassCannotBeInstantiatedFromOutside() throws Exception {
        Constructor<Requests> constructor = Requests.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}

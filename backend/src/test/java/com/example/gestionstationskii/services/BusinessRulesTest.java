package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated business tests: no Spring context, database or banking provider. */
class BusinessRulesTest {
    @Test
    void registrationRejectsCapacityReachedEvenIfStatusIsStale() {
        IRegistrationRepository registrations = mock(IRegistrationRepository.class);
        ISkierRepository skiers = mock(ISkierRepository.class);
        PlanningSlotRepository slots = mock(PlanningSlotRepository.class);
        IRegistrationServices service = new RegistrationServicesImpl(registrations, skiers, mock(ICourseRepository.class), slots,
                mock(IInstructorRepository.class), mock(EntityManager.class));
        Skier skier = new Skier(); skier.setNumSkier(1L); skier.setDateOfBirth(LocalDate.of(1990, 1, 1));
        Course course = new Course(); course.setNumCourse(1L); course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        PlanningSlot slot = new PlanningSlot(); slot.setId(2L); slot.setCourse(course); slot.setStatus(SlotStatus.OPEN); slot.setMaxCapacity(6);
        slot.setStartTime(LocalDateTime.now().plusDays(1)); slot.setEndTime(slot.getStartTime().plusHours(1));
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier)); when(slots.lockById(2L)).thenReturn(Optional.of(slot));
        when(registrations.findBySkierNumSkier(1L)).thenReturn(Collections.emptyList());
        when(registrations.findByPlanningSlotId(2L)).thenReturn(Arrays.asList(new Registration(), new Registration(), new Registration(), new Registration(), new Registration(), new Registration()));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.registerSlot(1L, 2L)).getStatus());
        verify(registrations, never()).save(any(Registration.class));
    }

    @Test
    void planningRejectsOverlappingInstructorAppointments() {
        PlanningSlotRepository slots = mock(PlanningSlotRepository.class);
        InstructorAvailabilityRepository availability = mock(InstructorAvailabilityRepository.class);
        IInstructorRepository instructors = mock(IInstructorRepository.class);
        ICourseRepository courses = mock(ICourseRepository.class);
        IPlanningServices service = new PlanningServicesImpl(slots, availability, instructors, courses, mock(IRegistrationRepository.class));
        LocalDate day = LocalDate.now().plusDays(2);
        Course course = new Course(); course.setNumCourse(1L); course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        Instructor instructor = new Instructor(); instructor.setNumInstructor(1L); instructor.setCourses(new HashSet<>(Collections.singletonList(course)));
        InstructorAvailability a = new InstructorAvailability(); a.setInstructor(instructor); a.setDayOfWeek(day.getDayOfWeek());
        a.setValidFrom(day); a.setValidUntil(day); a.setStartTime(LocalTime.of(8, 0)); a.setEndTime(LocalTime.of(18, 0));
        PlanningSlot existing = new PlanningSlot(); existing.setId(5L); existing.setInstructor(instructor); existing.setStatus(SlotStatus.OPEN);
        existing.setStartTime(day.atTime(10, 0)); existing.setEndTime(day.atTime(12, 0));
        when(courses.lockById(1L)).thenReturn(Optional.of(course)); when(instructors.lockById(1L)).thenReturn(Optional.of(instructor));
        when(instructors.findById(1L)).thenReturn(Optional.of(instructor)); when(availability.findByInstructorNumInstructor(1L)).thenReturn(Collections.singletonList(a));
        when(slots.findAll()).thenReturn(Collections.singletonList(existing));
        Requests.Slot input = new Requests.Slot(); input.setCourseId(1L); input.setInstructorId(1L); input.setMaxCapacity(6);
        input.setStartTime(day.atTime(11, 0)); input.setEndTime(day.atTime(13, 0));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.create(input)).getStatus());
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void paidPaymentCannotBecomeFailed() {
        PaymentRepository payments = mock(PaymentRepository.class);
        IPaymentServices service = new PaymentServicesImpl(payments, mock(RefundRepository.class), mock(ISkierRepository.class), mock(ISubscriptionRepository.class));
        Payment p = new Payment(); p.setStatus(PaymentStatus.PAID);
        when(payments.lockById(1L)).thenReturn(Optional.of(p));
        Requests.PaymentState input = new Requests.PaymentState(); input.setStatus(PaymentStatus.FAILED);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.changeStatus(1L, input)).getStatus());
        assertEquals(PaymentStatus.PAID, p.getStatus());
    }

    @Test
    void refundCannotExceedRemainingPaidAmount() {
        PaymentRepository payments = mock(PaymentRepository.class);
        RefundRepository refunds = mock(RefundRepository.class);
        IPaymentServices service = new PaymentServicesImpl(payments, refunds, mock(ISkierRepository.class), mock(ISubscriptionRepository.class));
        Payment p = new Payment(); p.setStatus(PaymentStatus.PARTIALLY_REFUNDED); p.setAmount(new BigDecimal("100.00")); p.setRefundedAmount(new BigDecimal("75.00"));
        when(payments.lockById(1L)).thenReturn(Optional.of(p));
        Requests.RefundInput input = new Requests.RefundInput(); input.setAmount(new BigDecimal("25.01")); input.setReason("Correction");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.refund(1L, input)).getStatus());
        verify(refunds, never()).save(any(Refund.class));
    }

    @Test
    void receiptPreservesPaymentContractWithoutReporting() {
        PaymentRepository payments = mock(PaymentRepository.class);
        RefundRepository refunds = mock(RefundRepository.class);
        IPaymentServices service = new PaymentServicesImpl(payments, refunds, mock(ISkierRepository.class), mock(ISubscriptionRepository.class));
        Skier skier = new Skier(); skier.setNumSkier(2L); skier.setFirstName("Amina"); skier.setLastName("Test");
        Subscription sub = new Subscription(); sub.setNumSub(3L);
        Payment p = new Payment(); p.setId(1L); p.setSkier(skier); p.setSubscription(sub); p.setReceiptNumber("SKI-TEST");
        p.setAmount(new BigDecimal("100.00")); p.setRefundedAmount(new BigDecimal("25.00")); p.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        p.setPaidAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        Refund r = new Refund(); r.setAmount(new BigDecimal("25.00"));
        when(payments.findById(1L)).thenReturn(Optional.of(p)); when(refunds.findByPaymentId(1L)).thenReturn(Collections.singletonList(r));
        PaymentReceipt receipt = service.receipt(1L);
        assertEquals("SKI-TEST", receipt.getReceiptNumber());
        assertEquals("Amina Test", receipt.getSkierName());
        assertEquals(new BigDecimal("75.00"), receipt.getNetAmount());
        assertEquals(Collections.singletonList(r), receipt.getRefunds());
    }

    @Test
    void monthlySubscriptionUsesCalendarMonthAtMonthEnd() {
        ISubscriptionRepository subscriptions = mock(ISubscriptionRepository.class);
        ISubscriptionServices service = new SubscriptionServicesImpl(subscriptions, mock(ISkierRepository.class), mock(PaymentRepository.class));
        Subscription s = new Subscription(); s.setTypeSub(TypeSubscription.MONTHLY); s.setStartDate(LocalDate.of(2028, 1, 31)); s.setPrice(50f);
        when(subscriptions.save(s)).thenReturn(s);
        assertEquals(LocalDate.of(2028, 2, 29), service.addSubscription(s).getEndDate());
    }

    @Test
    void adjacentLessonsDoNotOverlap() {
        LocalDateTime start = LocalDate.of(2027, 1, 1).atTime(9, 0);
        assertFalse(TimeRanges.overlaps(start, start.plusHours(1), start.plusHours(1), start.plusHours(2)));
        assertTrue(TimeRanges.overlaps(start, start.plusHours(1), start.plusMinutes(30), start.plusHours(2)));
    }

    @Test
    void skierCreationUsesSubscriptionAndRegistrationInterfaces() {
        ISkierRepository skiers = mock(ISkierRepository.class);
        ICourseRepository courses = mock(ICourseRepository.class);
        ISubscriptionServices subscriptions = mock(ISubscriptionServices.class);
        IRegistrationServices registrations = mock(IRegistrationServices.class);
        ISkierServices service = new SkierServicesImpl(skiers, mock(IPisteRepository.class), courses,
                mock(IRegistrationRepository.class), mock(ISubscriptionRepository.class), subscriptions, registrations, mock(PaymentRepository.class));
        Skier skier = new Skier(); skier.setFirstName("Amina"); skier.setLastName("Test"); skier.setCity("Tunis"); skier.setDateOfBirth(LocalDate.of(2000, 1, 1));
        Subscription sub = new Subscription(); skier.setSubscription(sub);
        PlanningSlot slot = new PlanningSlot(); slot.setId(3L);
        when(courses.existsById(2L)).thenReturn(true);
        when(subscriptions.addSubscription(sub)).thenReturn(sub);
        when(skiers.save(skier)).thenAnswer(invocation -> { skier.setNumSkier(1L); return skier; });
        when(registrations.nextAvailableSlot(2L)).thenReturn(slot);
        assertSame(skier, service.addSkierAndAssignToCourse(skier, 2L));
        verify(subscriptions).addSubscription(sub);
        verify(registrations).registerSlot(1L, 3L);
    }
}

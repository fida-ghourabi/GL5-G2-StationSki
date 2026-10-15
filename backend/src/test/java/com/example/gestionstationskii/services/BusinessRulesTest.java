package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.dto.ReportResponses;
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
    void rentalPriceExcludesReturnDateAndHandlesLeapDay() {
        EquipmentRentalServicesImpl service = new EquipmentRentalServicesImpl(mock(EquipmentRepository.class),
                mock(RentalRepository.class), mock(ISkierRepository.class), mock(EntityManager.class));
        assertEquals(new BigDecimal("25.00"), service.price(new BigDecimal("12.50"), LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1)));
    }

    @Test
    void activeRentalBlocksAnotherReservationEvenAfterPlannedEnd() {
        EquipmentRepository equipment = mock(EquipmentRepository.class);
        RentalRepository rentals = mock(RentalRepository.class);
        ISkierRepository skiers = mock(ISkierRepository.class);
        EquipmentRentalServicesImpl service = new EquipmentRentalServicesImpl(equipment, rentals, skiers, mock(EntityManager.class));
        Equipment e = new Equipment(); e.setId(1L); e.setStatus(EquipmentStatus.RENTED);
        Rental active = new Rental(); active.setStatus(RentalStatus.ACTIVE); active.setStartDate(LocalDate.now().minusDays(5)); active.setExpectedEndDate(LocalDate.now().minusDays(2));
        when(skiers.lockById(1L)).thenReturn(Optional.of(new Skier()));
        when(equipment.lockById(1L)).thenReturn(Optional.of(e));
        when(rentals.findByEquipment_Id(1L)).thenReturn(Collections.singletonList(active));
        Requests.RentalInput input = new Requests.RentalInput(); input.setSkierId(1L); input.setEquipmentId(1L);
        input.setStartDate(LocalDate.now().plusDays(1)); input.setExpectedEndDate(LocalDate.now().plusDays(3));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.reserve(input)).getStatus());
        verify(rentals, never()).save(any(Rental.class));
    }

    @Test
    void registrationRejectsCapacityReachedEvenIfStatusIsStale() {
        IRegistrationRepository registrations = mock(IRegistrationRepository.class);
        ISkierRepository skiers = mock(ISkierRepository.class);
        PlanningSlotRepository slots = mock(PlanningSlotRepository.class);
        RegistrationServicesImpl service = new RegistrationServicesImpl(registrations, skiers, mock(ICourseRepository.class), slots,
                mock(IInstructorRepository.class), mock(EntityManager.class));
        Skier skier = new Skier(); skier.setNumSkier(1L); skier.setDateOfBirth(LocalDate.of(1990, 1, 1));
        Course course = new Course(); course.setNumCourse(1L); course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        PlanningSlot slot = new PlanningSlot(); slot.setId(2L); slot.setCourse(course); slot.setStatus(SlotStatus.OPEN); slot.setMaxCapacity(6);
        slot.setStartTime(LocalDateTime.now().plusDays(1)); slot.setEndTime(slot.getStartTime().plusHours(1));
        when(skiers.lockById(1L)).thenReturn(Optional.of(skier)); when(slots.lockById(2L)).thenReturn(Optional.of(slot));
        when(registrations.findBySkier_NumSkier(1L)).thenReturn(Collections.emptyList());
        when(registrations.findByPlanningSlot_Id(2L)).thenReturn(Arrays.asList(new Registration(), new Registration(), new Registration(), new Registration(), new Registration(), new Registration()));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.registerSlot(1L, 2L)).getStatus());
        verify(registrations, never()).save(any(Registration.class));
    }

    @Test
    void planningRejectsOverlappingInstructorAppointments() {
        PlanningSlotRepository slots = mock(PlanningSlotRepository.class);
        InstructorAvailabilityRepository availability = mock(InstructorAvailabilityRepository.class);
        IInstructorRepository instructors = mock(IInstructorRepository.class);
        ICourseRepository courses = mock(ICourseRepository.class);
        PlanningServicesImpl service = new PlanningServicesImpl(slots, availability, instructors, courses, mock(IRegistrationRepository.class));
        LocalDate day = LocalDate.now().plusDays(2);
        Course course = new Course(); course.setNumCourse(1L); course.setTypeCourse(TypeCourse.COLLECTIVE_ADULT);
        Instructor instructor = new Instructor(); instructor.setNumInstructor(1L); instructor.setCourses(new HashSet<>(Collections.singletonList(course)));
        InstructorAvailability a = new InstructorAvailability(); a.setInstructor(instructor); a.setDayOfWeek(day.getDayOfWeek());
        a.setValidFrom(day); a.setValidUntil(day); a.setStartTime(LocalTime.of(8, 0)); a.setEndTime(LocalTime.of(18, 0));
        PlanningSlot existing = new PlanningSlot(); existing.setId(5L); existing.setInstructor(instructor); existing.setStatus(SlotStatus.OPEN);
        existing.setStartTime(day.atTime(10, 0)); existing.setEndTime(day.atTime(12, 0));
        when(courses.lockById(1L)).thenReturn(Optional.of(course)); when(instructors.lockById(1L)).thenReturn(Optional.of(instructor));
        when(instructors.findById(1L)).thenReturn(Optional.of(instructor)); when(availability.findByInstructor_NumInstructor(1L)).thenReturn(Collections.singletonList(a));
        when(slots.findAll()).thenReturn(Collections.singletonList(existing));
        Requests.Slot input = new Requests.Slot(); input.setCourseId(1L); input.setInstructorId(1L); input.setMaxCapacity(6);
        input.setStartTime(day.atTime(11, 0)); input.setEndTime(day.atTime(13, 0));
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.create(input)).getStatus());
        verify(slots, never()).save(any(PlanningSlot.class));
    }

    @Test
    void paidPaymentCannotBecomeFailed() {
        PaymentRepository payments = mock(PaymentRepository.class);
        PaymentServicesImpl service = new PaymentServicesImpl(payments, mock(RefundRepository.class), mock(ISkierRepository.class), mock(ISubscriptionRepository.class));
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
        PaymentServicesImpl service = new PaymentServicesImpl(payments, refunds, mock(ISkierRepository.class), mock(ISubscriptionRepository.class));
        Payment p = new Payment(); p.setStatus(PaymentStatus.PARTIALLY_REFUNDED); p.setAmount(new BigDecimal("100.00")); p.setRefundedAmount(new BigDecimal("75.00"));
        when(payments.lockById(1L)).thenReturn(Optional.of(p));
        Requests.RefundInput input = new Requests.RefundInput(); input.setAmount(new BigDecimal("25.01")); input.setReason("Correction");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class, () -> service.refund(1L, input)).getStatus());
        verify(refunds, never()).save(any(Refund.class));
    }

    @Test
    void revenuePlacesRefundInItsOwnAccountingPeriod() {
        PaymentRepository payments = mock(PaymentRepository.class);
        RefundRepository refunds = mock(RefundRepository.class);
        ReportServicesImpl service = new ReportServicesImpl(payments, refunds, mock(ISubscriptionRepository.class), mock(ICourseRepository.class), mock(PlanningSlotRepository.class), mock(IRegistrationRepository.class));
        Payment p = new Payment(); p.setAmount(new BigDecimal("100.00")); p.setStatus(PaymentStatus.REFUNDED); p.setPaidAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        Refund r = new Refund(); r.setAmount(new BigDecimal("100.00")); r.setRefundDate(LocalDateTime.of(2026, 2, 15, 10, 0));
        when(payments.findAll()).thenReturn(Collections.singletonList(p)); when(refunds.findAll()).thenReturn(Collections.singletonList(r));
        ReportResponses.Revenue january = service.revenue(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0));
        ReportResponses.Revenue february = service.revenue(LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 3, 1, 0, 0));
        assertEquals(0, new BigDecimal("100").compareTo(january.getNet()));
        assertEquals(0, new BigDecimal("-100").compareTo(february.getNet()));
    }

    @Test
    void monthlySubscriptionUsesCalendarMonthAtMonthEnd() {
        ISubscriptionRepository subscriptions = mock(ISubscriptionRepository.class);
        SubscriptionServicesImpl service = new SubscriptionServicesImpl(subscriptions, mock(ISkierRepository.class), mock(PaymentRepository.class));
        Subscription s = new Subscription(); s.setTypeSub(TypeSubscription.MONTHLY); s.setStartDate(LocalDate.of(2028, 1, 31)); s.setPrice(50f);
        when(subscriptions.save(s)).thenReturn(s);
        assertEquals(LocalDate.of(2028, 2, 29), service.addSubscription(s).getEndDate());
    }
}

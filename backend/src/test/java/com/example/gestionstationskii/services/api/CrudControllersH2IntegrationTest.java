package com.example.gestionstationskii.services.api;

import com.example.gestionstationskii.controllers.CourseRestController;
import com.example.gestionstationskii.controllers.InstructorRestController;
import com.example.gestionstationskii.controllers.PaymentRestController;
import com.example.gestionstationskii.controllers.PisteRestController;
import com.example.gestionstationskii.controllers.SkierRestController;
import com.example.gestionstationskii.controllers.SubscriptionRestController;
import com.example.gestionstationskii.dto.PaymentReceipt;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.services.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("crud-test")
@Transactional
class CrudControllersH2IntegrationTest {
    @Autowired private CourseRestController courses;
    @Autowired private InstructorRestController instructors;
    @Autowired private PaymentRestController payments;
    @Autowired private PisteRestController pistes;
    @Autowired private SkierRestController skiers;
    @Autowired private SubscriptionRestController subscriptions;

    @Test
    void courseAndInstructorDtoFlowCoversSearchAssignmentUpdateAndRemoval() {
        Requests.CourseInput courseInput = courseInput();
        Course course = courses.addCourse(courseInput);

        assertEquals(course.getNumCourse(), courses.getById(course.getNumCourse()).getNumCourse());
        assertEquals(1, courses.getAllCourses().size());
        assertEquals(1, courses.search(TypeCourse.INDIVIDUAL, Support.SKI, 2).size());
        assertTrue(courses.registrations(course.getNumCourse()).isEmpty());
        assertEquals(0, courses.participants(course.getNumCourse()));
        assertTrue(courses.instructors(course.getNumCourse()).isEmpty());

        Requests.InstructorInput instructorInput = instructorInput();
        Instructor instructor = instructors.addInstructor(instructorInput);
        assertEquals(1, instructors.search("amina").size());
        assertEquals(1, instructors.getAllInstructors().size());

        Instructor assigned = instructors.assign(instructor.getNumInstructor(), course.getNumCourse());
        assertEquals(1, assigned.getCourses().size());
        assertEquals(1, instructors.courses(instructor.getNumInstructor()).size());
        assertEquals(1, courses.instructors(course.getNumCourse()).size());

        Requests.CourseInput courseUpdate = courseInput();
        courseUpdate.setNumCourse(course.getNumCourse());
        courseUpdate.setPrice(175F);
        assertEquals(175F, courses.updateCourse(courseUpdate).getPrice());

        assertThrows(BusinessException.class, () -> courses.search(null, null, 0));

        Requests.InstructorInput instructorUpdate = instructorInput();
        instructorUpdate.setNumInstructor(instructor.getNumInstructor());
        instructorUpdate.setLastName("Updated");
        assertEquals("Updated", instructors.updateInstructor(instructorUpdate).getLastName());

        instructors.unassign(instructor.getNumInstructor(), course.getNumCourse());
        assertTrue(instructors.courses(instructor.getNumInstructor()).isEmpty());
        instructors.delete(instructor.getNumInstructor());
        courses.delete(course.getNumCourse());
        assertTrue(courses.getAllCourses().isEmpty());
    }

    @Test
    void subscriptionDtoFlowCoversTypeDateAndStatusBranches() {
        Requests.SubscriptionInput input = subscriptionInput(
                TypeSubscription.MONTHLY, LocalDate.of(2026, 1, 10), 50F);
        Subscription subscription = subscriptions.addSubscription(input);

        assertEquals(subscription.getNumSub(), subscriptions.getById(subscription.getNumSub()).getNumSub());
        assertEquals(1, subscriptions.getSubscriptionsByType(TypeSubscription.MONTHLY).size());
        assertEquals(1, subscriptions.getSubscriptionsByDates(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).size());
        assertEquals("UPCOMING", subscriptions.status(subscription.getNumSub(), LocalDate.of(2026, 1, 9)));
        assertEquals("ACTIVE", subscriptions.status(subscription.getNumSub(), LocalDate.of(2026, 1, 10)));
        assertEquals("EXPIRED", subscriptions.status(subscription.getNumSub(), LocalDate.of(2026, 2, 10)));

        Requests.SubscriptionInput update = subscriptionInput(
                TypeSubscription.ANNUAL, LocalDate.of(2026, 3, 1), 90F);
        update.setNumSub(subscription.getNumSub());
        assertEquals(TypeSubscription.ANNUAL,
                subscriptions.updateSubscription(update).getTypeSub());
        subscriptions.delete(subscription.getNumSub());
        assertTrue(subscriptions.getSubscriptionsByType(TypeSubscription.ANNUAL).isEmpty());
    }

    @Test
    void paymentDtoFlowCoversRecordStatusRefundHistoryAndReceipt() {
        Requests.SkierInput skierInput = skierInput();
        skierInput.setSubscription(subscriptionInput(
                TypeSubscription.MONTHLY, LocalDate.of(2026, 1, 1), 50F));
        Skier skier = skiers.addSkier(skierInput);
        Long subscriptionId = skier.getSubscription().getNumSub();

        Requests.PaymentInput paymentInput = new Requests.PaymentInput();
        paymentInput.setSkierId(skier.getNumSkier());
        paymentInput.setSubscriptionId(subscriptionId);
        paymentInput.setAmount(new BigDecimal("100.00"));
        paymentInput.setMethod(PaymentMethod.CARD);
        paymentInput.setTransactionReference("DTO-TEST-1");

        Payment payment = payments.recordPayment(paymentInput);
        assertEquals(PaymentStatus.PENDING, payments.get(payment.getId()).getStatus());
        assertEquals(1, payments.bySkier(skier.getNumSkier()).size());
        assertEquals(1, payments.bySubscription(subscriptionId).size());

        Requests.PaymentState state = new Requests.PaymentState();
        state.setStatus(PaymentStatus.PAID);
        assertEquals(PaymentStatus.PAID, payments.status(payment.getId(), state).getStatus());

        Requests.RefundInput refundInput = new Requests.RefundInput();
        refundInput.setAmount(new BigDecimal("25.00"));
        refundInput.setReason("DTO integration refund");
        assertNotNull(payments.refund(payment.getId(), refundInput).getId());
        assertEquals(1, payments.refunds(payment.getId()).size());

        PaymentReceipt receipt = payments.receipt(payment.getId());
        assertEquals(payment.getId(), receipt.getPaymentId());
        assertEquals(new BigDecimal("75.00"), receipt.getNetAmount());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, receipt.getStatus());
    }

    @Test
    void skierAndPisteDtoFlowCoversSubscriptionPisteSearchAndAssociationRules() {
        Requests.PisteInput pisteInput = pisteInput();
        Piste piste = pistes.addPiste(pisteInput);
        Requests.SkierInput skierInput = skierInput();
        Skier skier = skiers.addSkier(skierInput);

        Requests.SubscriptionInput subscriptionInput = subscriptionInput(
                TypeSubscription.SEMESTRIEL, LocalDate.of(2026, 1, 1), 120F);
        Subscription subscription = subscriptions.addSubscription(subscriptionInput);
        assertEquals(subscription.getNumSub(),
                skiers.assignToSubscription(skier.getNumSkier(), subscription.getNumSub()).getSubscription().getNumSub());

        assertEquals(1, pistes.search(Color.GREEN).size());

        Requests.PisteInput pisteUpdate = pisteInput();
        pisteUpdate.setNumPiste(piste.getNumPiste());
        pisteUpdate.setNamePiste("Updated DTO piste");
        assertEquals("Updated DTO piste", pistes.update(pisteUpdate).getNamePiste());
        assertEquals(1, skiers.search("amina", "tunis", 0, 10).getTotalElements());
        assertEquals(1, skiers.retrieveSkiersBySubscriptionType(TypeSubscription.SEMESTRIEL).size());

        assertEquals(skier.getNumSkier(),
                skiers.assignToPiste(skier.getNumSkier(), piste.getNumPiste()).getNumSkier());
        assertTrue(pistes.skiers(piste.getNumPiste()).stream()
                .anyMatch(assigned -> skier.getNumSkier().equals(assigned.getNumSkier())));
        assertEquals(1, pistes.search(Color.GREEN).size());

        BusinessException duplicateAssignment = assertThrows(BusinessException.class,
                () -> skiers.assignToPiste(skier.getNumSkier(), piste.getNumPiste()));
        assertEquals(HttpStatus.CONFLICT, duplicateAssignment.getStatus());
        BusinessException linkedPisteDeletion = assertThrows(BusinessException.class,
                () -> pistes.deleteById(piste.getNumPiste()));
        assertEquals(HttpStatus.CONFLICT, linkedPisteDeletion.getStatus());
    }

    @Test
    void paymentDtoRejectsDuplicateTransactionReference() {
        Payment payment = createPayment("DTO-DUPLICATE-REF");

        Requests.PaymentInput duplicate = paymentInputFor(payment);
        BusinessException duplicateReference = assertThrows(BusinessException.class,
                () -> payments.recordPayment(duplicate));
        assertEquals(HttpStatus.CONFLICT, duplicateReference.getStatus());
    }

    @Test
    void paymentDtoRejectsRefundAboveRemainingAmount() {
        Payment payment = createPayment("DTO-OVER-REFUND");
        Requests.PaymentState paid = new Requests.PaymentState();
        paid.setStatus(PaymentStatus.PAID);
        payments.status(payment.getId(), paid);

        Requests.RefundInput refund = new Requests.RefundInput();
        refund.setAmount(new BigDecimal("100.01"));
        refund.setReason("Too much");
        BusinessException excessiveRefund = assertThrows(BusinessException.class,
                () -> payments.refund(payment.getId(), refund));
        assertEquals(HttpStatus.CONFLICT, excessiveRefund.getStatus());
    }

    @Test
    void courseDtoRejectsChangingClassificationAfterInstructorAssignment() {
        Course course = courses.addCourse(courseInput());
        Instructor instructor = instructors.addInstructor(instructorInput());
        instructors.assign(instructor.getNumInstructor(), course.getNumCourse());

        Requests.CourseInput update = courseInput();
        update.setNumCourse(course.getNumCourse());
        update.setSupport(Support.SNOWBOARD);
        BusinessException immutableClassification = assertThrows(BusinessException.class,
                () -> courses.updateCourse(update));
        assertEquals(HttpStatus.CONFLICT, immutableClassification.getStatus());
    }

    private Payment createPayment(String transactionReference) {
        Requests.SkierInput skierInput = skierInput();
        skierInput.setSubscription(subscriptionInput(
                TypeSubscription.MONTHLY, LocalDate.of(2026, 1, 1), 50F));
        Skier skier = skiers.addSkier(skierInput);

        Requests.PaymentInput paymentInput = new Requests.PaymentInput();
        paymentInput.setSkierId(skier.getNumSkier());
        paymentInput.setSubscriptionId(skier.getSubscription().getNumSub());
        paymentInput.setAmount(new BigDecimal("100.00"));
        paymentInput.setMethod(PaymentMethod.CARD);
        paymentInput.setTransactionReference(transactionReference);
        return payments.recordPayment(paymentInput);
    }

    private Requests.PaymentInput paymentInputFor(Payment payment) {
        Requests.PaymentInput input = new Requests.PaymentInput();
        input.setSkierId(payment.getSkier().getNumSkier());
        input.setSubscriptionId(payment.getSubscription().getNumSub());
        input.setAmount(payment.getAmount());
        input.setMethod(payment.getMethod());
        input.setTransactionReference(payment.getTransactionReference());
        return input;
    }

    @Test
    void dtoValidationRejectsInvalidCourseInstructorSkierAndPiste() {
        Requests.CourseInput invalidCourse = courseInput();
        invalidCourse.setLevel(0);
        assertThrows(BusinessException.class, () -> courses.addCourse(invalidCourse));

        Requests.InstructorInput invalidInstructor = instructorInput();
        invalidInstructor.setDateOfHire(null);
        assertThrows(BusinessException.class, () -> instructors.addInstructor(invalidInstructor));

        Requests.SkierInput invalidSkier = skierInput();
        invalidSkier.setDateOfBirth(LocalDate.now().plusDays(1));
        assertThrows(BusinessException.class, () -> skiers.addSkier(invalidSkier));

        Requests.PisteInput invalidPiste = pisteInput();
        invalidPiste.setSlope(91);
        assertThrows(BusinessException.class, () -> pistes.addPiste(invalidPiste));
    }

    private Requests.CourseInput courseInput() {
        Requests.CourseInput input = new Requests.CourseInput();
        input.setLevel(2);
        input.setTypeCourse(TypeCourse.INDIVIDUAL);
        input.setSupport(Support.SKI);
        input.setPrice(100F);
        input.setTimeSlot(0);
        return input;
    }

    private Requests.InstructorInput instructorInput() {
        Requests.InstructorInput input = new Requests.InstructorInput();
        input.setFirstName("Amina");
        input.setLastName("Test");
        input.setDateOfHire(LocalDate.of(2020, 1, 1));
        return input;
    }

    private Requests.SubscriptionInput subscriptionInput(TypeSubscription type, LocalDate startDate, Float price) {
        Requests.SubscriptionInput input = new Requests.SubscriptionInput();
        input.setTypeSub(type);
        input.setStartDate(startDate);
        input.setPrice(price);
        return input;
    }

    private Requests.SkierInput skierInput() {
        Requests.SkierInput input = new Requests.SkierInput();
        input.setFirstName("Amina");
        input.setLastName("Test");
        input.setCity("Tunis");
        input.setDateOfBirth(LocalDate.of(2000, 1, 1));
        return input;
    }

    private Requests.PisteInput pisteInput() {
        Requests.PisteInput input = new Requests.PisteInput();
        input.setNamePiste("DTO piste");
        input.setColor(Color.GREEN);
        input.setLength(1500);
        input.setSlope(25);
        return input;
    }
}

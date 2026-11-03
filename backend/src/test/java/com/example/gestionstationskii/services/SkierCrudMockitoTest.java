package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated CRUD unit tests: every constructor dependency is mocked. */
@ExtendWith(MockitoExtension.class)
class SkierCrudMockitoTest {
    @Mock private ISkierRepository skierRepository;
    @Mock private IPisteRepository pisteRepository;
    @Mock private ICourseRepository courseRepository;
    @Mock private IRegistrationRepository registrationRepository;
    @Mock private ISubscriptionRepository subscriptionRepository;
    @Mock private ISubscriptionServices subscriptionServices;
    @Mock private IRegistrationServices registrationServices;
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private SkierServicesImpl service;

    private Skier skier() {
        Skier skier = new Skier();
        skier.setFirstName("Test");
        skier.setLastName("Skier");
        skier.setCity("Test city");
        skier.setDateOfBirth(LocalDate.of(2000, 1, 15));
        return skier;
    }

    private void verifyUnrelatedDependenciesUnused() {
        verifyNoInteractions(pisteRepository, courseRepository, subscriptionRepository,
                subscriptionServices, registrationServices);
    }

    @Test
    void createSkier() {
        Skier input = skier();
        Skier saved = skier();
        saved.setNumSkier(1L);
        when(skierRepository.save(input)).thenReturn(saved);

        Skier result = service.addSkier(input);

        assertSame(saved, result);
        assertEquals(1L, result.getNumSkier());
        assertNotNull(input.getPistes());
        assertTrue(input.getPistes().isEmpty());
        assertNotNull(input.getRegistrations());
        assertTrue(input.getRegistrations().isEmpty());
        verify(skierRepository).save(input);
        verifyNoMoreInteractions(skierRepository);
        verifyNoInteractions(registrationRepository, paymentRepository);
        verifyUnrelatedDependenciesUnused();
    }

    @Test
    void readSkier() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        when(skierRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(skierRepository.findAll()).thenReturn(Collections.singletonList(stored));

        assertSame(stored, service.retrieveSkier(1L));
        assertEquals(Collections.singletonList(stored), service.retrieveAllSkiers());

        verify(skierRepository).findById(1L);
        verify(skierRepository).findAll();
        verifyNoMoreInteractions(skierRepository);
        verifyNoInteractions(registrationRepository, paymentRepository);
        verifyUnrelatedDependenciesUnused();
    }

    @Test
    void updateSkier() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        Skier changes = skier();
        changes.setNumSkier(1L);
        changes.setFirstName("Updated");
        changes.setLastName("Tester");
        changes.setCity("Another city");
        changes.setDateOfBirth(LocalDate.of(1998, 5, 20));
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(skierRepository.save(stored)).thenReturn(stored);

        Skier result = service.updateSkier(changes);

        assertSame(stored, result);
        assertEquals(1L, result.getNumSkier());
        assertEquals("Updated", result.getFirstName());
        assertEquals("Tester", result.getLastName());
        assertEquals("Another city", result.getCity());
        assertEquals(LocalDate.of(1998, 5, 20), result.getDateOfBirth());
        verify(skierRepository).lockById(1L);
        verify(skierRepository).save(stored);
        verifyNoMoreInteractions(skierRepository);
        verifyNoInteractions(registrationRepository, paymentRepository);
        verifyUnrelatedDependenciesUnused();
    }

    @Test
    void updateSkierWithUnchangedCityAndRejectsMissingRecord() {
        Skier current = skier();
        current.setNumSkier(1L);
        Skier changes = skier();
        changes.setNumSkier(1L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(skierRepository.save(current)).thenReturn(current);

        assertSame(current, service.updateSkier(changes));
        assertThrows(BusinessException.class, () -> service.updateSkier(null));
        changes.setNumSkier(2L);
        when(skierRepository.lockById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.updateSkier(changes));
    }

    @Test
    void deleteSkier() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        when(skierRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(false);
        when(paymentRepository.existsBySkier_NumSkier(1L)).thenReturn(false);

        service.removeSkier(1L);

        verify(skierRepository).findById(1L);
        verify(registrationRepository).existsBySkier_NumSkier(1L);
        verify(paymentRepository).existsBySkier_NumSkier(1L);
        verify(skierRepository).delete(stored);
        verifyNoMoreInteractions(skierRepository, registrationRepository, paymentRepository);
        verifyUnrelatedDependenciesUnused();
    }

    @Test
    void createSkierWithNewSubscriptionDelegatesSubscriptionCreation() {
        Skier input = skier();
        Subscription newSubscription = new Subscription();
        Subscription savedSubscription = new Subscription();
        savedSubscription.setNumSub(4L);
        input.setSubscription(newSubscription);
        when(subscriptionServices.addSubscription(newSubscription)).thenReturn(savedSubscription);
        when(skierRepository.save(input)).thenReturn(input);

        assertSame(input, service.addSkier(input));
        assertSame(savedSubscription, input.getSubscription());
        verify(subscriptionServices).addSubscription(newSubscription);
        verify(skierRepository).save(input);
    }

    @Test
    void createSkierWithExistingSubscriptionUsesItsCurrentOwner() {
        Skier input = skier();
        Subscription requested = new Subscription();
        requested.setNumSub(6L);
        Subscription current = new Subscription();
        current.setNumSub(6L);
        input.setSubscription(requested);
        when(subscriptionServices.retrieveSubscriptionById(6L)).thenReturn(current);
        when(skierRepository.findBySubscription(current)).thenReturn(null);
        when(skierRepository.save(input)).thenReturn(input);

        assertSame(input, service.addSkier(input));
        assertSame(current, input.getSubscription());

        Skier secondInput = skier();
        secondInput.setSubscription(requested);
        when(skierRepository.findBySubscription(current)).thenReturn(skier());
        assertThrows(BusinessException.class, () -> service.addSkier(secondInput));
        verify(skierRepository, times(1)).save(input);
    }

    @Test
    void createSkierRejectsAssignedIdAndInvalidProfile() {
        Skier identified = skier();
        identified.setNumSkier(10L);
        assertThrows(BusinessException.class, () -> service.addSkier(identified));

        Skier invalid = skier();
        invalid.setFirstName("  ");
        assertThrows(BusinessException.class, () -> service.addSkier(invalid));

        Skier missingBirthDate = skier();
        missingBirthDate.setDateOfBirth(null);
        assertThrows(BusinessException.class, () -> service.addSkier(missingBirthDate));

        Skier bornToday = skier();
        bornToday.setDateOfBirth(LocalDate.now());
        assertThrows(BusinessException.class, () -> service.addSkier(bornToday));
        verifyNoInteractions(skierRepository, subscriptionServices);
    }

    @Test
    void assignExistingSubscriptionOnlyWhenUnowned() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        Subscription sub = new Subscription();
        sub.setNumSub(2L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.of(sub));
        when(skierRepository.findBySubscription(sub)).thenReturn(null);
        when(skierRepository.save(stored)).thenReturn(stored);

        assertSame(stored, service.assignSkierToSubscription(1L, 2L));
        assertSame(sub, stored.getSubscription());
        verify(skierRepository).save(stored);
    }

    @Test
    void assignSubscriptionRejectsMissingResourcesOrAlreadyOwnedSubscription() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.assignSkierToSubscription(1L, 2L));

        Skier stored = skier();
        stored.setNumSkier(1L);
        Subscription sub = new Subscription();
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.of(sub));
        when(skierRepository.findBySubscription(sub)).thenReturn(skier());
        assertThrows(BusinessException.class, () -> service.assignSkierToSubscription(1L, 2L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void assignSubscriptionRejectsMissingSubscription() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.assignSkierToSubscription(1L, 2L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void addSkierAndAssignToCourseRegistersNextAvailableSlot() {
        Skier input = skier();
        Skier saved = skier();
        saved.setNumSkier(1L);
        PlanningSlot slot = new PlanningSlot();
        slot.setId(3L);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(new com.example.gestionstationskii.entities.Course()));
        when(skierRepository.save(input)).thenReturn(saved);
        when(registrationServices.nextAvailableSlot(2L)).thenReturn(slot);

        assertSame(saved, service.addSkierAndAssignToCourse(input, 2L));
        verify(registrationServices).registerSlot(1L, 3L);
    }

    @Test
    void addSkierAndAssignToCourseRejectsMissingCourse() {
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        Skier input = skier();
        assertThrows(BusinessException.class, () -> service.addSkierAndAssignToCourse(input, 2L));
        verifyNoInteractions(skierRepository, registrationServices);
    }

    @Test
    void assignSkierToPisteInitializesCollectionAndRejectsDuplicate() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        stored.setPistes(null);
        com.example.gestionstationskii.entities.Piste piste = new com.example.gestionstationskii.entities.Piste();
        piste.setNumPiste(2L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(pisteRepository.findById(2L)).thenReturn(Optional.of(piste));
        when(skierRepository.save(stored)).thenReturn(stored);

        assertSame(stored, service.assignSkierToPiste(1L, 2L));
        assertEquals(new HashSet<>(Collections.singletonList(piste)), stored.getPistes());
        assertThrows(BusinessException.class, () -> service.assignSkierToPiste(1L, 2L));
    }

    @Test
    void assignSkierToPisteRejectsMissingSkierOrPiste() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.assignSkierToPiste(1L, 2L));

        Skier stored = skier();
        stored.setNumSkier(1L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(stored));
        when(pisteRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.assignSkierToPiste(1L, 2L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void subscriptionTypeAndPageSearchValidateInputs() {
        when(skierRepository.findBySubscription_TypeSub(com.example.gestionstationskii.entities.TypeSubscription.ANNUAL))
                .thenReturn(Collections.emptyList());
        assertTrue(service.retrieveSkiersBySubscriptionType(
                com.example.gestionstationskii.entities.TypeSubscription.ANNUAL).isEmpty());
        assertThrows(BusinessException.class, () -> service.retrieveSkiersBySubscriptionType(null));
        assertThrows(BusinessException.class, () -> service.search("name", "city", -1, 10));
        assertThrows(BusinessException.class, () -> service.search("name", "city", 0, 0));
        assertThrows(BusinessException.class, () -> service.search("name", "city", 0, 101));

        Page<Skier> expected = Page.empty(PageRequest.of(0, 10));
        when(skierRepository.search("name", "city", PageRequest.of(0, 10))).thenReturn(expected);
        assertSame(expected, service.search("name", "city", 0, 10));
    }

    @Test
    void deleteSkierRejectsRegistrationOrPaymentHistoryAndReadRejectsMissing() {
        Skier stored = skier();
        stored.setNumSkier(1L);
        when(skierRepository.findById(1L)).thenReturn(Optional.of(stored));
        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.removeSkier(1L));

        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(false);
        when(paymentRepository.existsBySkier_NumSkier(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.removeSkier(1L));

        when(skierRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.retrieveSkier(2L));
        verify(skierRepository, never()).delete(any(Skier.class));
    }
}

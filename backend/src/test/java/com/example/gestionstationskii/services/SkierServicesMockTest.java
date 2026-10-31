package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.example.gestionstationskii.services.BusinessAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkierServicesMockTest {

    @Mock private ISkierRepository skierRepository;
    @Mock private IPisteRepository pisteRepository;
    @Mock private ICourseRepository courseRepository;
    @Mock private IRegistrationRepository registrationRepository;
    @Mock private ISubscriptionRepository subscriptionRepository;
    @Mock private ISubscriptionServices subscriptionServices;
    @Mock private IRegistrationServices registrationServices;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private SkierServicesImpl service;

    private static Skier skier(Long id) {
        Skier s = new Skier();
        s.setNumSkier(id);
        s.setFirstName("Sahar");
        s.setLastName("Saidani");
        s.setCity("Tunis");
        s.setDateOfBirth(LocalDate.of(2000, 1, 1));
        return s;
    }

    private static Subscription subscription(Long id) {
        Subscription sub = new Subscription();
        sub.setNumSub(id);
        sub.setTypeSub(TypeSubscription.ANNUAL);
        return sub;
    }

    private static Piste piste(Long id) {
        Piste p = new Piste();
        p.setNumPiste(id);
        p.setNamePiste("Piste " + id);
        return p;
    }

    // ---------- retrieveAllSkiers / retrieveSkier ----------

    @Test
    void retrieveAllSkiersReturnsRepositoryContent() {
        when(skierRepository.findAll()).thenReturn(Arrays.asList(skier(1L), skier(2L)));

        assertEquals(2, service.retrieveAllSkiers().size());
        verify(skierRepository).findAll();
    }

    @Test
    void retrieveSkierReturnsExistingSkier() {
        Skier s = skier(1L);
        when(skierRepository.findById(1L)).thenReturn(Optional.of(s));

        assertSame(s, service.retrieveSkier(1L));
    }

    @Test
    void retrieveSkierFailsWhenMissing() {
        when(skierRepository.findById(9L)).thenReturn(Optional.<Skier>empty());

        assertNotFound(() -> service.retrieveSkier(9L));
    }

    @Test
    void retrieveSkierRejectsInvalidId() {
        assertBadRequest(() -> service.retrieveSkier(null));
        assertBadRequest(() -> service.retrieveSkier(0L));
        assertBadRequest(() -> service.retrieveSkier(-3L));
        verifyNoInteractions(skierRepository);
    }

    // ---------- addSkier ----------

    @Test
    void addSkierSavesSkierWithoutSubscriptionAndInitializesCollections() {
        Skier input = skier(null);
        when(skierRepository.save(any(Skier.class))).thenAnswer(inv -> inv.getArgument(0));

        Skier saved = service.addSkier(input);

        assertNull(saved.getSubscription());
        assertNotNull(saved.getPistes());
        assertTrue(saved.getPistes().isEmpty());
        assertNotNull(saved.getRegistrations());
        assertTrue(saved.getRegistrations().isEmpty());
        verifyNoInteractions(subscriptionServices);
    }

    @Test
    void addSkierRejectsPresetIdentifier() {
        assertBadRequest(() -> service.addSkier(skier(5L)));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void addSkierRejectsInvalidFields() {
        Skier noFirstName = skier(null);
        noFirstName.setFirstName("  ");
        assertBadRequest(() -> service.addSkier(noFirstName));

        Skier noLastName = skier(null);
        noLastName.setLastName(null);
        assertBadRequest(() -> service.addSkier(noLastName));

        Skier noCity = skier(null);
        noCity.setCity("");
        assertBadRequest(() -> service.addSkier(noCity));

        Skier tooLongCity = skier(null);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) sb.append('a');
        tooLongCity.setCity(sb.toString());
        assertBadRequest(() -> service.addSkier(tooLongCity));

        Skier noBirth = skier(null);
        noBirth.setDateOfBirth(null);
        assertBadRequest(() -> service.addSkier(noBirth));

        Skier futureBirth = skier(null);
        futureBirth.setDateOfBirth(LocalDate.now().plusDays(2));
        assertBadRequest(() -> service.addSkier(futureBirth));

        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void addSkierCreatesNewSubscriptionWhenItHasNoIdentifier() {
        Skier input = skier(null);
        Subscription newSub = subscription(null);
        Subscription createdSub = subscription(11L);
        input.setSubscription(newSub);
        when(subscriptionServices.addSubscription(newSub)).thenReturn(createdSub);
        when(skierRepository.save(any(Skier.class))).thenAnswer(inv -> inv.getArgument(0));

        Skier saved = service.addSkier(input);

        assertSame(createdSub, saved.getSubscription());
        verify(subscriptionServices).addSubscription(newSub);
        verify(subscriptionServices, never()).retrieveSubscriptionById(anyLong());
    }

    @Test
    void addSkierReusesExistingFreeSubscription() {
        Skier input = skier(null);
        Subscription ref = subscription(12L);
        Subscription existing = subscription(12L);
        input.setSubscription(ref);
        when(subscriptionServices.retrieveSubscriptionById(12L)).thenReturn(existing);
        when(skierRepository.findBySubscription(existing)).thenReturn(null);
        when(skierRepository.save(any(Skier.class))).thenAnswer(inv -> inv.getArgument(0));

        Skier saved = service.addSkier(input);

        assertSame(existing, saved.getSubscription());
        verify(subscriptionServices, never()).addSubscription(any(Subscription.class));
    }

    @Test
    void addSkierRejectsSubscriptionAlreadyOwnedBySomeoneElse() {
        Skier input = skier(null);
        Subscription existing = subscription(12L);
        input.setSubscription(subscription(12L));
        when(subscriptionServices.retrieveSubscriptionById(12L)).thenReturn(existing);
        when(skierRepository.findBySubscription(existing)).thenReturn(skier(99L));

        assertConflict(() -> service.addSkier(input));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    // ---------- assignSkierToSubscription ----------

    @Test
    void assignSkierToSubscriptionLinksFreeSubscription() {
        Skier s = skier(1L);
        Subscription sub = subscription(2L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(s));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.of(sub));
        when(skierRepository.findBySubscription(sub)).thenReturn(null);
        when(skierRepository.save(s)).thenReturn(s);

        Skier result = service.assignSkierToSubscription(1L, 2L);

        assertSame(sub, result.getSubscription());
        verify(skierRepository).save(s);
    }

    @Test
    void assignSkierToSubscriptionRejectsInvalidIds() {
        assertBadRequest(() -> service.assignSkierToSubscription(null, 2L));
        assertBadRequest(() -> service.assignSkierToSubscription(1L, 0L));
    }

    @Test
    void assignSkierToSubscriptionFailsWhenSkierMissing() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.<Skier>empty());

        assertNotFound(() -> service.assignSkierToSubscription(1L, 2L));
    }

    @Test
    void assignSkierToSubscriptionFailsWhenSubscriptionMissing() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(skier(1L)));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.<Subscription>empty());

        assertNotFound(() -> service.assignSkierToSubscription(1L, 2L));
    }

    @Test
    void assignSkierToSubscriptionRejectsAlreadyAssignedSubscription() {
        Subscription sub = subscription(2L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(skier(1L)));
        when(subscriptionRepository.lockById(2L)).thenReturn(Optional.of(sub));
        when(skierRepository.findBySubscription(sub)).thenReturn(skier(7L));

        assertConflict(() -> service.assignSkierToSubscription(1L, 2L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    // ---------- addSkierAndAssignToCourse ----------

    @Test
    void addSkierAndAssignToCourseRegistersOnNextAvailableSlot() {
        Skier input = skier(null);
        PlanningSlot slot = new PlanningSlot();
        slot.setId(7L);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(new Course()));
        when(skierRepository.save(any(Skier.class))).thenAnswer(inv -> {
            Skier s = inv.getArgument(0);
            s.setNumSkier(10L);
            return s;
        });
        when(registrationServices.nextAvailableSlot(3L)).thenReturn(slot);

        Skier saved = service.addSkierAndAssignToCourse(input, 3L);

        assertEquals(Long.valueOf(10L), saved.getNumSkier());
        verify(registrationServices).registerSlot(10L, 7L);
    }

    @Test
    void addSkierAndAssignToCourseFailsWhenCourseMissing() {
        when(courseRepository.findById(3L)).thenReturn(Optional.<Course>empty());

        assertNotFound(() -> service.addSkierAndAssignToCourse(skier(null), 3L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void addSkierAndAssignToCourseRejectsInvalidCourseId() {
        assertBadRequest(() -> service.addSkierAndAssignToCourse(skier(null), null));
        verifyNoInteractions(courseRepository);
    }

    // ---------- removeSkier ----------

    @Test
    void removeSkierDeletesSkierWithoutHistory() {
        Skier s = skier(1L);
        when(skierRepository.findById(1L)).thenReturn(Optional.of(s));
        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(false);
        when(paymentRepository.existsBySkier_NumSkier(1L)).thenReturn(false);

        service.removeSkier(1L);

        verify(skierRepository).delete(s);
    }

    @Test
    void removeSkierRefusesWhenRegistrationsExist() {
        when(skierRepository.findById(1L)).thenReturn(Optional.of(skier(1L)));
        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(true);

        assertConflict(() -> service.removeSkier(1L));
        verify(skierRepository, never()).delete(any(Skier.class));
    }

    @Test
    void removeSkierRefusesWhenPaymentsExist() {
        when(skierRepository.findById(1L)).thenReturn(Optional.of(skier(1L)));
        when(registrationRepository.existsBySkier_NumSkier(1L)).thenReturn(false);
        when(paymentRepository.existsBySkier_NumSkier(1L)).thenReturn(true);

        assertConflict(() -> service.removeSkier(1L));
        verify(skierRepository, never()).delete(any(Skier.class));
    }

    @Test
    void removeSkierFailsWhenMissing() {
        when(skierRepository.findById(1L)).thenReturn(Optional.<Skier>empty());

        assertNotFound(() -> service.removeSkier(1L));
    }

    // ---------- assignSkierToPiste ----------

    @Test
    void assignSkierToPisteAddsPisteWhenCollectionIsNull() {
        Skier s = skier(1L);
        s.setPistes(null);
        Piste p = piste(4L);
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(s));
        when(pisteRepository.findById(4L)).thenReturn(Optional.of(p));
        when(skierRepository.save(s)).thenReturn(s);

        Skier result = service.assignSkierToPiste(1L, 4L);

        assertTrue(result.getPistes().contains(p));
        verify(skierRepository).save(s);
    }

    @Test
    void assignSkierToPisteRejectsDuplicateAssignment() {
        Skier s = skier(1L);
        Piste p = piste(4L);
        s.setPistes(new HashSet<>(Collections.singleton(p)));
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(s));
        when(pisteRepository.findById(4L)).thenReturn(Optional.of(p));

        assertConflict(() -> service.assignSkierToPiste(1L, 4L));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void assignSkierToPisteFailsWhenSkierOrPisteMissing() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.<Skier>empty());
        assertNotFound(() -> service.assignSkierToPiste(1L, 4L));

        when(skierRepository.lockById(2L)).thenReturn(Optional.of(skier(2L)));
        when(pisteRepository.findById(4L)).thenReturn(Optional.<Piste>empty());
        assertNotFound(() -> service.assignSkierToPiste(2L, 4L));
    }

    @Test
    void assignSkierToPisteRejectsInvalidIds() {
        assertBadRequest(() -> service.assignSkierToPiste(null, 4L));
        assertBadRequest(() -> service.assignSkierToPiste(1L, -1L));
    }

    // ---------- retrieveSkiersBySubscriptionType ----------

    @Test
    void retrieveSkiersBySubscriptionTypeDelegatesToRepository() {
        when(skierRepository.findBySubscription_TypeSub(TypeSubscription.MONTHLY))
                .thenReturn(Collections.singletonList(skier(1L)));

        List<Skier> result = service.retrieveSkiersBySubscriptionType(TypeSubscription.MONTHLY);

        assertEquals(1, result.size());
    }

    @Test
    void retrieveSkiersBySubscriptionTypeRejectsNull() {
        assertBadRequest(() -> service.retrieveSkiersBySubscriptionType(null));
    }

    // ---------- updateSkier ----------

    @Test
    void updateSkierCopiesPersonalFieldsOnly() {
        Skier current = skier(1L);
        Subscription keep = subscription(5L);
        current.setSubscription(keep);
        Skier input = skier(1L);
        input.setFirstName("Nouveau");
        input.setLastName("Nom");
        input.setCity("Sfax");
        input.setDateOfBirth(LocalDate.of(1995, 5, 5));
        when(skierRepository.lockById(1L)).thenReturn(Optional.of(current));
        when(skierRepository.save(current)).thenReturn(current);

        Skier result = service.updateSkier(input);

        assertEquals("Nouveau", result.getFirstName());
        assertEquals("Nom", result.getLastName());
        assertEquals("Sfax", result.getCity());
        assertEquals(LocalDate.of(1995, 5, 5), result.getDateOfBirth());
        assertSame(keep, result.getSubscription());
    }

    @Test
    void updateSkierRejectsMissingIdentifierAndInvalidFields() {
        assertBadRequest(() -> service.updateSkier(skier(null)));

        Skier invalid = skier(1L);
        invalid.setFirstName(null);
        assertBadRequest(() -> service.updateSkier(invalid));
        verify(skierRepository, never()).save(any(Skier.class));
    }

    @Test
    void updateSkierFailsWhenMissing() {
        when(skierRepository.lockById(1L)).thenReturn(Optional.<Skier>empty());

        assertNotFound(() -> service.updateSkier(skier(1L)));
    }

    // ---------- search ----------

    @Test
    void searchDelegatesWithPageRequest() {
        Page<Skier> page = new PageImpl<>(Collections.singletonList(skier(1L)));
        when(skierRepository.search(eq("sa"), eq("tunis"), any(Pageable.class))).thenReturn(page);

        Page<Skier> result = service.search("sa", "tunis", 0, 20);

        assertEquals(1, result.getTotalElements());
        verify(skierRepository).search(eq("sa"), eq("tunis"), any(Pageable.class));
    }

    @Test
    void searchRejectsInvalidPagination() {
        assertBadRequest(() -> service.search("", "", -1, 10));
        assertBadRequest(() -> service.search("", "", 0, 0));
        assertBadRequest(() -> service.search("", "", 0, 101));
        verifyNoInteractions(skierRepository);
    }
}

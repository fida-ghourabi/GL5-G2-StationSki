package com.example.gestionstationskii.services.skier;

import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.services.ISkierServices;
import com.example.gestionstationskii.services.SkierServicesImpl;
import com.example.gestionstationskii.controllers.SkierRestController;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Skier;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

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
    @Mock private ISkierServices controllerService;
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
    void createSkierControllerMapsDtoToEntity() {
        Requests.SkierInput input = skierInput();
        when(controllerService.addSkier(any(Skier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skier result = new SkierRestController(controllerService).addSkier(input);

        assertEquals(input.getFirstName(), result.getFirstName());
        assertEquals(input.getLastName(), result.getLastName());
        assertEquals(input.getCity(), result.getCity());
        assertEquals(input.getDateOfBirth(), result.getDateOfBirth());
        verify(controllerService).addSkier(argThat(skier ->
                skier.getNumSkier() == null
                        && input.getFirstName().equals(skier.getFirstName())
                        && input.getLastName().equals(skier.getLastName())
                        && input.getCity().equals(skier.getCity())
                        && input.getDateOfBirth().equals(skier.getDateOfBirth())));
        verifyNoMoreInteractions(controllerService);
    }

    @Test
    void updateSkierControllerMapsDtoToEntity() {
        Requests.SkierInput input = skierInput();
        input.setNumSkier(9L);
        input.setFirstName("Updated via DTO");
        when(controllerService.updateSkier(any(Skier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skier result = new SkierRestController(controllerService).update(input);

        assertEquals(9L, result.getNumSkier());
        assertEquals("Updated via DTO", result.getFirstName());
        verify(controllerService).updateSkier(argThat(skier ->
                skier.getNumSkier().equals(9L)
                        && "Updated via DTO".equals(skier.getFirstName())
                        && input.getLastName().equals(skier.getLastName())
                        && input.getCity().equals(skier.getCity())
                        && input.getDateOfBirth().equals(skier.getDateOfBirth())));
        verifyNoMoreInteractions(controllerService);
    }

    private Requests.SkierInput skierInput() {
        Requests.SkierInput input = new Requests.SkierInput();
        input.setFirstName("Test");
        input.setLastName("Skier");
        input.setCity("Test city");
        input.setDateOfBirth(LocalDate.of(2000, 1, 15));
        return input;
    }
}

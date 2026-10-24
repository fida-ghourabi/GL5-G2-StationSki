package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import javax.persistence.EntityManager;
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
    void adjacentLessonsDoNotOverlap() {
        LocalDateTime start = LocalDate.of(2027, 1, 1).atTime(9, 0);
        assertFalse(TimeRanges.overlaps(start, start.plusHours(1), start.plusHours(1), start.plusHours(2)));
        assertTrue(TimeRanges.overlaps(start, start.plusHours(1), start.plusMinutes(30), start.plusHours(2)));
    }

}

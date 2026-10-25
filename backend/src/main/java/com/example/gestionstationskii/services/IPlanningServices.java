package com.example.gestionstationskii.services;

import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.InstructorAvailability;
import com.example.gestionstationskii.entities.PlanningSlot;

import java.time.LocalDateTime;
import java.util.List;

public interface IPlanningServices {
    InstructorAvailability addAvailability(Long instructorId, Requests.Availability input);

    List<InstructorAvailability> availability(Long instructorId);

    PlanningSlot get(Long id);

    PlanningSlot create(Requests.Slot input);

    PlanningSlot update(Long id, Requests.Slot input);

    void cancel(Long id);

    List<PlanningSlot> search(LocalDateTime from, LocalDateTime to, Long courseId, Long instructorId, boolean available);
}

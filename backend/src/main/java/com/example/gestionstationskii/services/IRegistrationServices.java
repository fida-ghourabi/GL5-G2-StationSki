package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.*;
import java.util.List;

public interface IRegistrationServices {
    Registration registerSlot(Long skierId, Long slotId);
    void cancel(Long id);
    List<Registration> bySkier(Long id);
    List<Registration> search(java.time.LocalDateTime from, java.time.LocalDateTime to, Long courseId);

	Registration addRegistrationAndAssignToSkier(Registration registration, Long numSkier);
	Registration assignRegistrationToCourse(Long numRegistration, Long numCourse);
	Registration addRegistrationAndAssignToSkierAndCourse(Registration registration, Long numSkieur, Long numCours);
	List<Integer> numWeeksCourseOfInstructorBySupport(Long numInstructor, Support support);
}


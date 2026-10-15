package com.example.gestionstationskii.services;


import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.Course;

import java.util.List;
import java.util.Set;

public interface IInstructorServices {
    void removeInstructor(Long id);
    Instructor assignCourse(Long id, Long courseId);
    Instructor unassignCourse(Long id, Long courseId);
    Set<Course> courses(Long id);
    List<Instructor> search(String name);

    Instructor addInstructor(Instructor instructor);

    List<Instructor> retrieveAllInstructors();

    Instructor updateInstructor(Instructor instructor);

    Instructor retrieveInstructor(Long numInstructor);

    Instructor addInstructorAndAssignToCourse(Instructor instructor, Long numCourse);

}

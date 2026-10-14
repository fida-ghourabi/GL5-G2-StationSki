package com.example.gestionstationskii.services;


import com.example.gestionstationskii.entities.Instructor;

import java.util.List;

public interface IInstructorServices {
    void removeInstructor(Long id);
    Instructor assignCourse(Long id, Long courseId);
    Instructor unassignCourse(Long id, Long courseId);
    java.util.Set<com.example.gestionstationskii.entities.Course> courses(Long id);
    List<Instructor> search(String name);

    Instructor addInstructor(Instructor instructor);

    List<Instructor> retrieveAllInstructors();

    Instructor updateInstructor(Instructor instructor);

    Instructor retrieveInstructor(Long numInstructor);

    Instructor addInstructorAndAssignToCourse(Instructor instructor, Long numCourse);

}

package com.example.gestionstationskii.services;


import com.example.gestionstationskii.entities.Course;

import java.util.List;

public interface ICourseServices {
    void removeCourse(Long id);
    List<Course> search(com.example.gestionstationskii.entities.TypeCourse type, com.example.gestionstationskii.entities.Support support, Integer level);
    List<com.example.gestionstationskii.entities.Registration> registrations(Long id);
    long participants(Long id);
    List<com.example.gestionstationskii.entities.Instructor> instructors(Long id);

    List<Course> retrieveAllCourses();

    Course  addCourse(Course  course);

    Course updateCourse(Course course);

    Course retrieveCourse(Long numCourse);


}

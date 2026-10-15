package com.example.gestionstationskii.services;


import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.entities.Instructor;
import com.example.gestionstationskii.entities.Registration;
import com.example.gestionstationskii.entities.Support;
import com.example.gestionstationskii.entities.TypeCourse;

import java.util.List;

public interface ICourseServices {
    void removeCourse(Long id);
    List<Course> search(TypeCourse type, Support support, Integer level);
    List<Registration> registrations(Long id);
    long participants(Long id);
    List<Instructor> instructors(Long id);

    List<Course> retrieveAllCourses();

    Course addCourse(Course course);

    Course updateCourse(Course course);

    Course retrieveCourse(Long numCourse);


}

package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.entities.Course;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.services.ICourseServices;
import com.example.gestionstationskii.entities.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Tag(name = "\uD83D\uDCDA Course Management")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseRestController {

    private final ICourseServices courseServices;

    @Operation(summary = "Supprimer un cours sans associations ni historique")
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) { courseServices.removeCourse(id); }

    @Operation(summary = "Filtrer les cours par type, support et niveau")
    @GetMapping("/search")
    public List<Course> search(@RequestParam(required = false) TypeCourse type,
            @RequestParam(required = false) Support support, @RequestParam(required = false) Integer level) {
        return courseServices.search(type, support, level);
    }
    @Operation(summary = "Consulter l'historique des inscriptions d'un cours")
    @GetMapping("/{id}/registrations")
    public List<Registration> registrations(@PathVariable Long id) { return courseServices.registrations(id); }

    @Operation(summary = "Nombre de skieurs distincts inscrits et non annulés")
    @GetMapping("/{id}/participants/count")
    public long participants(@PathVariable Long id) { return courseServices.participants(id); }

    @Operation(summary = "Consulter le moniteur associé au cours")
    @GetMapping("/{id}/instructors")
    public List<Instructor> instructors(@PathVariable Long id) { return courseServices.instructors(id); }

    @Operation(description = "Add Course")
    @PostMapping("/add")
    public Course addCourse(@RequestBody Requests.CourseInput input){
        return courseServices.addCourse(input.toEntity());
    }

    @Operation(description = "Retrieve all Courses")
    @GetMapping("/all")
    public List<Course> getAllCourses(){
        return courseServices.retrieveAllCourses();
    }

    @Operation(description = "Update Course ")
    @PutMapping("/update")
    public Course updateCourse(@RequestBody Requests.CourseInput input){
        return courseServices.updateCourse(input.toEntity());
    }

    @Operation(description = "Retrieve Course by Id")
    @GetMapping("/get/{id-course}")
    public Course getById(@PathVariable("id-course") Long numCourse){
        return courseServices.retrieveCourse(numCourse);
    }

}

package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.gestionstationskii.dto.Requests;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.gestionstationskii.entities.*;


import java.util.List;

@Tag(name = "\uD83D\uDC69\u200D\uD83C\uDFEB Instructor Management")
@RestController
@RequestMapping("/instructor")
@RequiredArgsConstructor
public class InstructorRestController {

    private final IInstructorServices instructorServices;

    @Operation(summary = "Supprimer un moniteur sans associations")
    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) { instructorServices.removeInstructor(id); }

    @Operation(summary = "Associer un cours existant à un moniteur")
    @PutMapping("/{id}/courses/{courseId}")
    public Instructor assign(@PathVariable Long id, @PathVariable Long courseId) { return instructorServices.assignCourse(id, courseId); }

    @Operation(summary = "Retirer un cours après annulation des créneaux futurs")
    @DeleteMapping("/{id}/courses/{courseId}")
    public Instructor unassign(@PathVariable Long id, @PathVariable Long courseId) { return instructorServices.unassignCourse(id, courseId); }

    @Operation(summary = "Lister les cours d'un moniteur")
    @GetMapping("/{id}/courses")
    public java.util.Set<Course> courses(@PathVariable Long id) { return instructorServices.courses(id); }

    @Operation(summary = "Rechercher un moniteur par nom ou prénom")
    @GetMapping("/search")
    public List<Instructor> search(@RequestParam(defaultValue = "") String name) { return instructorServices.search(name); }

    @Operation(description = "Add Instructor")
    @PostMapping("/add")
    public Instructor addInstructor(@RequestBody Requests.InstructorInput instructor){
        return  instructorServices.addInstructor(instructor.toEntity());
    }
    @Operation(description = "Add Instructor and Assign To Course")
    @PutMapping("/addAndAssignToCourse/{numCourse}")
    public Instructor addAndAssignToInstructor(@RequestBody Requests.InstructorInput instructor, @PathVariable("numCourse")Long numCourse){
        return  instructorServices.addInstructorAndAssignToCourse(instructor.toEntity(),numCourse);
    }
    @Operation(description = "Retrieve all Instructors")
    @GetMapping("/all")
    public List<Instructor> getAllInstructors(){
        return instructorServices.retrieveAllInstructors();
    }

    @Operation(description = "Update Instructor ")
    @PutMapping("/update")
    public Instructor updateInstructor(@RequestBody Requests.InstructorInput instructor){
        return  instructorServices.updateInstructor(instructor.toEntity());
    }

    @Operation(description = "Retrieve Instructor by Id")
    @GetMapping("/get/{id-instructor}")
    public Instructor getById(@PathVariable("id-instructor") Long numInstructor){
        return instructorServices.retrieveInstructor(numInstructor);
    }

}
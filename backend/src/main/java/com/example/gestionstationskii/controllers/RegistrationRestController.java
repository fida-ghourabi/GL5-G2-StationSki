package com.example.gestionstationskii.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.gestionstationskii.dto.Requests;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.gestionstationskii.services.*;
import com.example.gestionstationskii.entities.*;

import java.util.List;

@Tag(name = "\uD83D\uDDD3️Registration Management")
@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationRestController {
    private final IRegistrationServices registrationServices;

    @Operation(summary = "Inscrire un skieur à un créneau disponible")
    @PostMapping("/skier/{skierId}/slot/{slotId}")
    public Registration register(@PathVariable Long skierId, @PathVariable Long slotId) { return registrationServices.registerSlot(skierId, slotId); }

    @Operation(summary = "Historique des inscriptions d'un skieur")
    @GetMapping("/skier/{skierId}")
    public List<Registration> bySkier(@PathVariable Long skierId) { return registrationServices.bySkier(skierId); }

    @Operation(summary = "Annuler une inscription en conservant son historique")
    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id) { registrationServices.cancel(id); }

    @Operation(summary = "Rechercher les inscriptions datées sur une période [from,to[")
    @GetMapping
    public List<Registration> search(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(required = false) Long courseId) { return registrationServices.search(from, to, courseId); }

    @Operation(description = "Add Registration and Assign to Skier")
    @PutMapping("/addAndAssignToSkier/{numSkieur}")
    public Registration addAndAssignToSkier(@RequestBody Requests.RegistrationInput registration,
                                            @PathVariable("numSkieur") Long numSkieur)
    {
        return  registrationServices.addRegistrationAndAssignToSkier(registration.toEntity(),numSkieur);
    }
    @Operation(description = "Assign Registration to Course")
    @PutMapping("/assignToCourse/{numRegis}/{numSkieur}")
    public Registration assignToCourse( @PathVariable("numRegis") Long numRegistration,
                                        @PathVariable("numSkieur") Long numCourse){
        return registrationServices.assignRegistrationToCourse(numRegistration, numCourse);
    }


    @Operation(description = "Add Registration and Assign to Skier and Course")
    @PutMapping("/addAndAssignToSkierAndCourse/{numSkieur}/{numCourse}")
    public Registration addAndAssignToSkierAndCourse(@RequestBody Requests.RegistrationInput registration,
                                                     @PathVariable("numSkieur") Long numSkieur,
                                                     @PathVariable("numCourse") Long numCourse)
    {
        return  registrationServices.addRegistrationAndAssignToSkierAndCourse(registration.toEntity(),numSkieur,numCourse);
    }

    @Operation(description = "Numbers of the weeks when an instructor has given lessons in a given support")
    @GetMapping("/numWeeks/{numInstructor}/{support}")
    public List<Integer> numWeeksCourseOfInstructorBySupport(@PathVariable("numInstructor")Long numInstructor,
                                                             @PathVariable("support") Support support) {
        return registrationServices.numWeeksCourseOfInstructorBySupport(numInstructor,support);
    }
}